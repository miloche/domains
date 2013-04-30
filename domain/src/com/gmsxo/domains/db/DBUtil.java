package com.gmsxo.domains.db;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.ImprovedNamingStrategy;
import org.hibernate.service.ServiceRegistryBuilder;

import com.gmsxo.domains.data.DnsServer;
import com.gmsxo.domains.data.Domain;
import com.gmsxo.domains.data.IpAddress;
 
public class DBUtil {
 private static final SessionFactory sessionFactory;
 
 private static final String DBADDR="jdbc:postgresql://localhost:5432/domains";
 //private static final String DBADDR="jdbc:postgresql://192.168.1.102:5432/domains";

 static {
  System.out.println("DBADDR:"+DBADDR);
  try {
    Configuration configuration = new Configuration()
      .setProperty("hibernate.connection.driver_class", "org.postgresql.Driver")
      .setProperty("hibernate.connection.url", DBADDR)
      .setProperty("hibernate.connection.username", "domains")
      .setProperty("hibernate.connection.password", "*****")
      .setProperty("hibernate.connection.provider_class", "org.hibernate.service.jdbc.connections.internal.C3P0ConnectionProvider")
      
      .setProperty("hibernate.hbm2ddl.auto", "validate")
      .setProperty("hibernate.dialect", "org.hibernate.dialect.PostgreSQL82Dialect")
      
      .setProperty("hibernate.ejb.naming_strategy", "org.hibernate.cfg.ImprovedNamingStrategy")
      
      .setProperty("hibernate.jdbc.batch_size", "100")

      .setProperty("hibernate.cache.provider_class", "org.hibernate.cache.EhCacheProvider")
      .setProperty("hibernate.cache.use_second_level_cache", "true")
      .setProperty("hibernate.cache.region.factory_class", "org.hibernate.cache.ehcache.EhCacheRegionFactory")
      .setProperty("hibernate.cache.use_query_cache", "true")
      .setProperty("net.sf.ehcache.configurationResourceName", "/com/gmsxo/domains/db/ehcache.xml")
      
      .setProperty("hibernate.show_sql", "false")
      .setProperty("hibernate.id.new_generator_mappings", "true")
      
      .setProperty("hibernate.c3p0.min_size", "5")
      .setProperty("hibernate.c3p0.max_size", "50")
      .setProperty("hibernate.c3p0.timeout", "300")
      .setProperty("hibernate.c3p0.max_statements", "200")
      .setProperty("hibernate.c3p0.idle_test_period", "3000")
      .setProperty("hibernate.debugUnreturnedConnectionStackTraces", "true")
      .setProperty("hibernate.c3p0.unreturnedConnectionTimeout", "1000")
      .setProperty("hibernate.current_session_context_class", "thread")
      
      
      
      .setNamingStrategy(ImprovedNamingStrategy.INSTANCE)
      .addPackage("com.gmsxo.domains.data")
      .addAnnotatedClass(Domain.class)
      .addAnnotatedClass(IpAddress.class)
      .addAnnotatedClass(DnsServer.class);
    sessionFactory = configuration
      .buildSessionFactory(new ServiceRegistryBuilder().applySettings(configuration.getProperties()).buildServiceRegistry());
    
    if (configuration.getProperty("hibernate.hbm2ddl.auto").equals("create")) {
      try {
        Session session = sessionFactory.openSession();
        session.beginTransaction();
        session.createSQLQuery("create unique index DOMAIN_DNS_SERVER_LNK_DOMAIN_ID_IDX on domain_dns_server_lnk (domain_id, dns_server_id)").executeUpdate();
        session.createSQLQuery("create index DOMAIN_DNS_SERVER_LNK_DNS_SERVER_ID_IDX on domain_dns_server_lnk (dns_server_id, domain_id)").executeUpdate();
        session.createSQLQuery("ALTER TABLE domain ALTER COLUMN id SET DEFAULT nextval('domain_id_seq')").executeUpdate();
        session.createSQLQuery("ALTER TABLE dns_server ALTER COLUMN id SET DEFAULT nextval('dns_server_id_seq')").executeUpdate();
        session.createSQLQuery("ALTER TABLE domain_dns_server_lnk ALTER COLUMN id SET DEFAULT nextval('domain_dns_server_lnk_id_seq')").executeUpdate();
        session.createSQLQuery("ALTER TABLE ip_address ALTER COLUMN id SET DEFAULT nextval('ip_address_id_seq')").executeUpdate();
        
        
        for (int id=1;id<=IpAddress.errors.length;id++) {
          session.createSQLQuery("insert into ip_address (id,ip_address) values ("+id+",'"+IpAddress.errors[id-1]+"')").executeUpdate();
          if (id>1) DBFacade.errors.add((IpAddress)session.get(IpAddress.class, (long)id));
        }
        DBFacade.setNullIP((IpAddress)session.get(IpAddress.class, 1l));
        System.out.println(DBFacade.errors.get(0));
        System.out.println(DBFacade.getNullIP());
        session.getTransaction().commit();
        session.flush();
        session.close();
      }
      catch (Exception e) {
        System.out.println("ERR");
        e.printStackTrace();
        System.exit(100);
      }
    } else {
      /*try {
        Session session = sessionFactory.openSession();
        session.beginTransaction();
        @SuppressWarnings("unchecked")
        List<IPAddress> list = (List<IPAddress>)session.createQuery("from IPAddress where id<100").list();
        for (IPAddress ipAddress:list) DBFacade.errors.add(ipAddress);
        
        session.getTransaction().commit();
        session.flush();
        session.close();
      }
      catch (Exception e) {
        System.out.println("ERR");
        e.printStackTrace();
        System.exit(100);
      }*/
    }
    System.out.println("DB INIT FINISHED");
  }
  catch (Throwable ex) {
   // Make sure you log the exception, as it might be swallowed
   System.err.println("Initial SessionFactory creation failed." + ex);
   throw new ExceptionInInitializerError(ex);
  }
 }
 public static SessionFactory getSessionFactory() {
  return sessionFactory;
 }
 public static Session openSession() {
   return sessionFactory.openSession();
 }
 
 public static void close() {
   sessionFactory.close();
 }
}