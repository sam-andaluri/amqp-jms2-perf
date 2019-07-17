package com.aws.jms2.amqp.perf;

import javax.jms.JMSContext;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.Message;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.jms.JMSConsumer;
import javax.jms.JMSException;
import javax.jms.MessageListener;
import javax.jms.TextMessage;
import javax.jms.CompletionListener;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.ConcurrentHashMap;

import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.UUID;
import java.util.Iterator;

import java.io.File;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.io.FileInputStream;
import java.io.ObjectInputStream;

import org.apache.commons.lang3.RandomStringUtils;

import com.codahale.metrics.*;


public class Producer {
    
    static JMSContext jmsContext = null;
    static Context context = null;
    static ConnectionFactory cf = null;
    static final MetricRegistry metrics = new MetricRegistry();
    static ConsoleReporter reporter = null;
    static ConcurrentHashMap<String, String> msgIds = new ConcurrentHashMap<String, String>();
    static Set<String> msgIdSet = msgIds.newKeySet();
    
    public static void startReport() {
      reporter = ConsoleReporter.forRegistry(metrics)
          .convertRatesTo(TimeUnit.SECONDS)
          .convertDurationsTo(TimeUnit.MILLISECONDS)
          .build();
      reporter.start(1, TimeUnit.SECONDS);
    }  
    
    public static void stopReport() {
        reporter.stop();
    }
    
    public static boolean recoverMsgIdSet() {
        boolean retVal = false;
        try {
            File file = new File("/tmp/msgidset.ser");
            if (file.exists()) {
                 FileInputStream fileIn = new FileInputStream("/tmp/msgidset.ser");
                ObjectInputStream in = new ObjectInputStream(fileIn);
                msgIdSet = (Set<String>)in.readObject();
                in.close();
                fileIn.close();
                retVal = true;
            }
        } catch (Exception ex) {
            System.out.println("recoverMsgIdSet " + ex.getMessage());
        }
        return retVal;
    }
    
    public static void generateMsgIdSet(int count) {
        for (int i = 0; i < count; i++) {
            msgIdSet.add(UUID.randomUUID().toString());
        }
    }
    
    public static void saveMsgIdSet() {
        try {
            FileOutputStream fileOut = new FileOutputStream("/tmp/msgidset.ser");
            ObjectOutputStream out = new ObjectOutputStream(fileOut);
            out.writeObject(msgIdSet);
            out.close();
            fileOut.close();            
        } catch (Exception ex) {
            System.out.println("recoverMsgIdSet " + ex.getMessage());
        }
    }
    
    public static void wait5Seconds() {
      try {
          Thread.sleep(5*1000);
      }
      catch(Exception ex) {
          System.out.println("wait5Seconds " + ex.getMessage());
      }
    }
    
    public static boolean isNullOrEmpty(String str) {
        if(str != null && !str.isEmpty())
            return false;
        return true;
    }
    
    public static int getNumMessages() {
        int numMessages = 1;
	    String nmsg = System.getProperty("NMSG");    
	    if (!isNullOrEmpty(nmsg))
	        numMessages = Integer.parseInt(nmsg);
	    return numMessages;
    }
    
    public static int getNumDestinations() {
        int numDest = 1;
	    String ndest = System.getProperty("NDST");    
	    if (!isNullOrEmpty(ndest))
	        numDest = Integer.parseInt(ndest);
	    return numDest;
    }
    
    public static int getMsgsPerProducer() {
        return (int) (getNumMessages()/getNumThreads());
    }
    
    public static int getAckMode() {
        int ackMode = JMSContext.AUTO_ACKNOWLEDGE;
        String ackM = System.getProperty("ACK");
	    if(!isNullOrEmpty(ackM)) {
	        switch(ackM) {
	            case "CLI" : 
	                ackMode = JMSContext.CLIENT_ACKNOWLEDGE;
	                break;
	            case "DUP" :
	                ackMode = JMSContext.DUPS_OK_ACKNOWLEDGE;
	                break;	     
	            case "TXN" :
	                ackMode = JMSContext.SESSION_TRANSACTED;
	                break;	  	    
	        }
	    }
	    return ackMode;
    }
    
    public static String getUserId() {
        return System.getProperty("USER");
    }
    
    public static String getUserPassword() {
        return System.getProperty("PASSWORD");
    }
    
    public static boolean getPersist() {
        boolean persist = true;   
        String psist = System.getProperty("PERS");    
	    if(!isNullOrEmpty(psist))
	        persist = Boolean.parseBoolean(psist);
	   return persist;
    }
    
    public static int getMessageSize() {
        int msgSize = 1024; 
        String msize = System.getProperty("SIZE");
	    if (!isNullOrEmpty(msize))
	        msgSize = Integer.parseInt(msize);
	    return msgSize;
    }
    
    public static int getNumThreads() {
        int numThreads = 1;
	    String nthr = System.getProperty("NTHR");
	    if (!isNullOrEmpty(nthr))
	        numThreads = Integer.parseInt(nthr);
	    return numThreads;
    }
    
    public static String getPayload() {
        return RandomStringUtils.randomAlphanumeric(getMessageSize());
    }
    
    public static Destination getDestination() {
        Destination queue = null;
        try {
            queue = (Destination)getInitialContext().lookup ("queueName");
        } catch (Exception ex) {
            System.out.println("getDestination " + ex.getMessage());
        }
        return queue; 
    }
    
    public static Context getInitialContext() {
        if (context == null) {
            try {
                context = new InitialContext();
            } catch(Exception ex) {
                System.out.println("getInitialContext " + ex.getMessage());
            }
        }
        return context;
    }
    public static ConnectionFactory getConnectionFactory() {
        if (cf == null) {
            try {
        		cf = (ConnectionFactory)getInitialContext().lookup("brokerURI");
            } catch(Exception ex) {
                System.out.println("getConnectionFactory " + ex.getMessage());
            }            
        }
        return cf;
    }
    public static JMSContext getJMSContext() {
        if (jmsContext == null) {
            try {
                String userId = getUserId();
                String userPassword = getUserPassword();
                if (!isNullOrEmpty(userId) && !isNullOrEmpty(userPassword))
                    jmsContext = getConnectionFactory().createContext(userId, userPassword);
                else 
                    jmsContext = getConnectionFactory().createContext(getAckMode());                
            } catch(Exception ex) {
                System.out.println("getJMSContext " + ex.getMessage());
            }
        }
        return jmsContext;    
    }
    
    //For testing recovery scenarios
    public static void runSingleThreadedSyncTest() {
        msgIdSet.clear();
        if (!recoverMsgIdSet()) {
            generateMsgIdSet(getNumMessages());
        }
        startReport();
        Meter requests = metrics.meter("requests");
        Iterator<String> itr = msgIdSet.iterator();
        boolean noError = true;
        while (itr.hasNext() && noError) {
            String uuid = itr.next();
            try {
                getJMSContext().createProducer().send(getDestination(), uuid);
                msgIdSet.remove(uuid);
                requests.mark();
            } catch (Exception ex) {
                System.out.println("runSingleThreadedSyncTest " + ex.getMessage());
                noError = false;
            }
        }
        saveMsgIdSet();
        stopReport();
        wait5Seconds(); 
    }
    
    //For performance testing
    public static void runThreadedAsyncTest() {
        ExecutorService executor = Executors.newFixedThreadPool(getNumThreads());
        startReport();
        Meter requests = metrics.meter("requests");
        Set<Callable<Integer>> callables = new HashSet<Callable<Integer>>();
        for (int i = 0; i < getNumThreads(); i++) {
            callables.add(new Callable<Integer>() {
                public Integer call() throws Exception {
                    int j = 0;
                    for (j = 0; j < getMsgsPerProducer(); j++) {
                        getJMSContext().createProducer().setAsync(null).send(getDestination(), getPayload());
                        requests.mark();                        
                    }
                    return j;
                }
            });
        }
        try {
        List<Future<Integer>> futures = executor.invokeAll(callables);
        } catch(Exception ex) {
            System.out.println("runThreadedAsyncTest " + ex.getMessage());
        }
        stopReport();
        wait5Seconds(); 
        executor.shutdown();
    }

	public static void main(String[] args) throws Exception {
        //runThreadedAsyncTest();
        runSingleThreadedSyncTest();
        System.exit(0);
	}
}
