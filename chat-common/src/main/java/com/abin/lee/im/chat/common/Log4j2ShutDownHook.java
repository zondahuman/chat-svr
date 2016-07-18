package com.abin.lee.im.chat.common;//package com.abin.lee.im.common.util;
//
//import org.apache.logging.log4j.LogManager;
//import org.apache.logging.log4j.Logger;
//import org.apache.logging.log4j.core.LoggerContext;
//import org.apache.logging.log4j.core.config.Configurator;
//
//import java.io.File;
//
///**
// * Created with IntelliJ IDEA.
// * User: tinkpad
// * Date: 16-5-28
// * Time: 下午7:50
// * To change this template use File | Settings | File Templates.
// */
//public class Log4j2ShutDownHook extends Thread{
//    private static Logger logger = LogManager.getLogger(Log4j2ShutDownHook.class);
//    private static String loggerFileName = "log4j2.xml";
//
//    @Override
//    public void run() {
//        logger.info("Log4j2ShutDownHook-start");
//        shutDownHook();
//        logger.info("Log4j2ShutDownHook-end");
//    }
//
//    public static void shutDownHook(){
//        String loggerPath = Log4j2ShutDownHook.class.getClassLoader().getResource(loggerFileName).getPath();
//        File file = new File(loggerPath);
//        if(!file.exists()){
//            logger = LogManager.getRootLogger();
//        } else {
//            System.setProperty("log4j.configurationFile", file.toURI().toString());
//            logger = LogManager.getLogger("yourProgramName");
//        }
//
//        Runtime.getRuntime().addShutdownHook(new Thread() {
//            public void run() {
//                logger.info("Shutting down - closing application");
//                // Shut down everything (e.g. threads) that you need to.
//
//                // then shut down log4j
//                if( LogManager.getContext() instanceof LoggerContext) {
//                    logger.debug("Shutting down log4j2");
//                    Configurator.shutdown((LoggerContext) LogManager.getContext());
//                } else
//                    logger.warn("Unable to shutdown log4j2");
//
//                // logger not usable anymore
//                logger.info("Done.");
//            }
//        });
//    }
//
//}
