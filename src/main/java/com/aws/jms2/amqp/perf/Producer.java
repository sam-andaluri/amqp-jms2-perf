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

import org.apache.commons.lang3.RandomStringUtils;

import com.codahale.metrics.*;


public class Producer {
    
    static JMSContext jmsContext = null;
    static Context context = null;
    static ConnectionFactory cf = null;
    static final MetricRegistry metrics = new MetricRegistry();
    static ConsoleReporter reporter = null;
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
    
    public static void wait5Seconds() {
      try {
          Thread.sleep(5*1000);
      }
      catch(InterruptedException e) {}
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
            
        }
        return queue; 
    }
    
    public static Context getInitialContext() {
        if (context == null) {
            try {
                context = new InitialContext();
            } catch(Exception ex) {
                
            }
        }
        return context;
    }
    public static ConnectionFactory getConnectionFactory() {
        if (cf == null) {
            try {
        		cf = (ConnectionFactory)getInitialContext().lookup("brokerURI");
            } catch(Exception ex) {
                
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
                
            }
        }
        return jmsContext;    
    }

	public static void main(String[] args) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(getNumThreads());
        startReport();
        Meter requests = metrics.meter("requests");
        for (int i = 0; i < getNumMessages(); i++) {
            executor.submit(new Runnable() {
            @Override
                public void run() {
                    getJMSContext().createProducer().setAsync(null).send(getDestination(), getPayload());
                    requests.mark();
                }
            });
        }
        stopReport();
        wait5Seconds();
        //executor.shutdown();
        //exit(0);
	}
}
