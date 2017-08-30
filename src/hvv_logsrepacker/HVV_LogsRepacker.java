/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package hvv_logsrepacker;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.swing.JOptionPane;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

/**
 *
 * @author yaroslav
 */
public class HVV_LogsRepacker {

    static final Logger logger = Logger.getLogger(HVV_LogsRepacker.class);
    static HVV_LogsRepackerSettings m_pSettings;
    
    static public Date GetLocalDate() {
        Date dt = new Date( System.currentTimeMillis() - 1000 * 60 * 60 * m_pSettings.GetTimeZoneShift());
        return dt;
    }
    
    /**
     * Функция для сообщения пользователю информационного сообщения
     * @param strMessage сообщение
     * @param strTitleBar заголовок
     */
    public static void MessageBoxInfo( String strMessage, String strTitleBar)
    {
        JOptionPane.showMessageDialog( null, strMessage, strTitleBar, JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * Функция для сообщения пользователю сообщения об ошибке
     * @param strMessage сообщение
     * @param strTitleBar заголовок
     */
    public static void MessageBoxError( String strMessage, String strTitleBar)
    {
        JOptionPane.showMessageDialog( null, strMessage, strTitleBar, JOptionPane.ERROR_MESSAGE);
    }
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        String strAMSrootEnvVar = System.getenv( "AMS_ROOT");
        
        //настройка логгера
        String strlog4jPropertiesFile = strAMSrootEnvVar + "/etc/log4j.logs.repacker.properties";
        File file = new File( strlog4jPropertiesFile);
        if(!file.exists())
            System.out.println("It is not possible to load the given log4j properties file :" + file.getAbsolutePath());
        else
            PropertyConfigurator.configure( file.getAbsolutePath());
        
        logger.info( "Logs repacker. v2017.08.30.13-00");
        logger.info( "START");
        
        m_pSettings = new HVV_LogsRepackerSettings( strAMSrootEnvVar);
        
        TreeMap mapFiles = new TreeMap();
        
        logger.info( "");
        logger.info( "*******");
        logger.info( "Создание списка файлов для упаковки");
        
        File folder = new File( strAMSrootEnvVar + "/logs");
        for( final File fileEntry : folder.listFiles()) {
            if( !fileEntry.isDirectory()) {
                String strFileName = fileEntry.getName();

                String [] strFileNamePartsDot = strFileName.split( "\\.");
                String [] strFileNamePartsMinus = strFileName.split( "-");
                if( strFileNamePartsDot.length == 3 && strFileNamePartsMinus.length == 2) {

                    int nIndexMinus = strFileName.indexOf( "-");
                    int nIndexDot = strFileName.indexOf( ".", nIndexMinus);
                    String strDatePart = strFileName.substring( nIndexMinus + 1, nIndexDot);

                    if( strDatePart.length() == 8) {
                        String strYear = strDatePart.substring( 0 , 4);
                        String strMonth = strDatePart.substring( 4, 6);
                        String strDay = strDatePart.substring( 6, 8);

                        int nYear  = Integer.parseInt( strYear);
                        int nMonth = Integer.parseInt( strMonth);
                        int nDay   = Integer.parseInt( strDay);

                        GregorianCalendar dt1 = new GregorianCalendar( nYear, nMonth-1, nDay);
                        //String strDt1 = "" + nYear + "." + nMonth + "." + nDay;
                        String strDt1 = String.format( "%02d.%02d.%02d", nYear, nMonth, nDay);

                        long ldt1 = dt1.getTimeInMillis();
                        long ldtn = GetLocalDate().getTime();
                        double lLifeLong = (( double) ( ldtn - ldt1)) / 1000. / 3600. / 24.;
                        if( lLifeLong >= 2.) {

                            logger.info( "Имя файла '" + strFileName + "' содержит 2 точки и один минус, и дата, описанная в нём, была более двух дней назад (файл достаточно стар). Отправляем в упаковщик.");

                            if( mapFiles.containsKey( strDt1)) {
                                String strFiles = ( String) mapFiles.get( strDt1);
                                strFiles += " " + strFileName;
                                mapFiles.put( strDt1, strFiles);
                            }
                            else {
                                mapFiles.put( strDt1, strFileName);
                            }
                        }
                        else {
                            logger.info( "Имя файла '" + strFileName + "' содержит 2 точки и один минус, но дата, описанная в нём, была менее двух дней назад (файл свеж).");
                        }
                    }
                    else {
                        logger.info( "Выделенная часть ответственная за дату = '" + strDatePart + "', и длина этой части не равна 8");
                    }
                }
                else {
                    logger.info( "Имя файла '" + strFileName + "' не содержит 2 точки и один минус.");
                }

            }
            else {
                logger.info( "Файл '" + fileEntry + "' это директория.");
            }
        }



        logger.info( "");
        logger.info( "*******");
        logger.info( "Упаковка отобранных файлов логов");
        
        Set set = mapFiles.entrySet();
        Iterator it = set.iterator();

        while( it.hasNext()) {
            Map.Entry entry = ( Map.Entry) it.next();
            String strKey = ( String) entry.getKey();
            String strFiles = ( String) entry.getValue();

            logger.debug( "key='" + strKey + "'. Values='" + strFiles + "'");

            FileOutputStream fos;
            try {
                fos = new FileOutputStream( strAMSrootEnvVar + "/logs/" + strKey + ".zip");
                ZipOutputStream zos = new ZipOutputStream(fos);

                String [] arrFiles = strFiles.split( " ");
                for( final String strFileNameToPack : arrFiles) {

                    //logger.info( ">>> " + strFileNameToPack);
                    
                    file = new File( strAMSrootEnvVar + "/logs/" + strFileNameToPack);
                    FileInputStream fis = new FileInputStream( file);
                    ZipEntry zipEntry = new ZipEntry( strFileNameToPack);
                    zos.putNextEntry( zipEntry);

                    byte[] bytes = new byte[1024];
                    int length;
                    while ((length = fis.read(bytes)) >= 0) {
                        zos.write( bytes, 0, length);
                    }

                    zos.closeEntry();
                    fis.close();
                }

                zos.close();
                fos.close();

            } catch (FileNotFoundException ex) {
                logger.error( "FileNotFoundException caught!", ex);
            } catch (IOException ex) {
                logger.error( "IOException caught!", ex);
            }

        }

        
        logger.info( "");
        logger.info( "*******");
        logger.info( "Удаление отобранных файлов логов");
        
        
        
        it = set.iterator();
        while( it.hasNext()) {
            Map.Entry entry = ( Map.Entry) it.next();
            String strKey = ( String) entry.getKey();
            String strFiles = ( String) entry.getValue();

            logger.debug( "key='" + strKey + "'. Values='" + strFiles + "'");

            FileOutputStream fos;

            String [] arrFiles = strFiles.split( " ");
            for( final String strFileNameToPack : arrFiles) {
                
                //logger.info( ">>> " + strFileNameToPack);
                
                File f = new File( strAMSrootEnvVar + "/logs/" + strFileNameToPack);
                f.delete();
            }

        }
        
        logger.info( "FINISH");
    }
    
}
