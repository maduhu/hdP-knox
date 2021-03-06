/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.hadoop.gateway;

import com.mycila.xmltool.XMLDoc;
import com.mycila.xmltool.XMLTag;
import org.apache.directory.server.protocol.shared.transport.TcpTransport;
import org.apache.hadoop.gateway.security.ldap.SimpleLdapDirectoryServer;
import org.apache.hadoop.gateway.services.DefaultGatewayServices;
import org.apache.hadoop.gateway.services.ServiceLifecycleException;
import org.apache.hadoop.gateway.util.KnoxCLI;
import org.apache.log4j.Appender;
import org.hamcrest.Matchers;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertThat;

;

public class KnoxCliSysBindTest {

  private static Class RESOURCE_BASE_CLASS = KnoxCliSysBindTest.class;
  private static Logger LOG = LoggerFactory.getLogger( KnoxCliSysBindTest.class );

  public static Enumeration<Appender> appenders;
  public static GatewayTestConfig config;
  public static GatewayServer gateway;
  public static String gatewayUrl;
  public static String clusterUrl;
  public static SimpleLdapDirectoryServer ldap;
  public static TcpTransport ldapTransport;

  private static final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
  private static final ByteArrayOutputStream errContent = new ByteArrayOutputStream();
  private static final String uuid = UUID.randomUUID().toString();

  @BeforeClass
  public static void setupSuite() throws Exception {
    System.setOut(new PrintStream(outContent));
    System.setErr(new PrintStream(errContent));
    setupLdap();
    setupGateway();
  }

  @AfterClass
  public static void cleanupSuite() throws Exception {
    ldap.stop( true );

    //FileUtils.deleteQuietly( new File( config.getGatewayHomeDir() ) );
    //NoOpAppender.tearDown( appenders );
  }

  public static void setupLdap( ) throws Exception {
    URL usersUrl = getResourceUrl( "users.ldif" );
    int port = findFreePort();
    ldapTransport = new TcpTransport( port );
    ldap = new SimpleLdapDirectoryServer( "dc=hadoop,dc=apache,dc=org", new File( usersUrl.toURI() ), ldapTransport );
    ldap.start();
    LOG.info( "LDAP port = " + ldapTransport.getPort() );
  }

  public static void setupGateway() throws Exception {

    File targetDir = new File( System.getProperty( "user.dir" ), "target" );
    File gatewayDir = new File( targetDir, "gateway-home-" + uuid );
    gatewayDir.mkdirs();

    GatewayTestConfig testConfig = new GatewayTestConfig();
    config = testConfig;
    testConfig.setGatewayHomeDir( gatewayDir.getAbsolutePath() );

    File topoDir = new File( testConfig.getGatewayTopologyDir() );
    topoDir.mkdirs();

    File deployDir = new File( testConfig.getGatewayDeploymentDir() );
    deployDir.mkdirs();

    writeTopology(topoDir, "test-cluster-1.xml", "guest", "guest-password", true);
    writeTopology(topoDir, "test-cluster-2.xml", "sam", "sam-password", true);
    writeTopology(topoDir, "test-cluster-3.xml", "admin", "admin-password", true);
    writeTopology(topoDir, "test-cluster-4.xml", "", "", false);


    DefaultGatewayServices srvcs = new DefaultGatewayServices();
    Map<String,String> options = new HashMap<String,String>();
    options.put( "persist-master", "false" );
    options.put( "master", "password" );
    try {
      srvcs.init( testConfig, options );
    } catch ( ServiceLifecycleException e ) {
      e.printStackTrace(); // I18N not required.
    }
  }

  private static void writeTopology(File topoDir, String name, String user, String pass, boolean goodTopology) throws Exception {
    File descriptor = new File(topoDir, name);

    if(descriptor.exists()){
      descriptor.delete();
      descriptor = new File(topoDir, name);
    }

    FileOutputStream stream = new FileOutputStream( descriptor, false );

    if(goodTopology) {
      createTopology(user, pass).toStream( stream );
    } else {
      createBadTopology().toStream( stream );
    }

    stream.close();

  }


  private static int findFreePort() throws IOException {
    ServerSocket socket = new ServerSocket(0);
    int port = socket.getLocalPort();
    socket.close();
    return port;
  }

  public static InputStream getResourceStream( String resource ) throws IOException {
    return getResourceUrl( resource ).openStream();
  }

  public static URL getResourceUrl( String resource ) {
    URL url = ClassLoader.getSystemResource( getResourceName( resource ) );
    assertThat( "Failed to find test resource " + resource, url, Matchers.notNullValue() );
    return url;
  }

  public static String getResourceName( String resource ) {
    return getResourceBaseName() + resource;
  }

  public static String getResourceBaseName() {
    return RESOURCE_BASE_CLASS.getName().replaceAll( "\\.", "/" ) + "/";
  }

  private static XMLTag createBadTopology(){
    XMLTag xml = XMLDoc.newDocument(true)
        .addRoot("topology")
        .addTag( "gateway" )
        .addTag("provider")
        .addTag("role").addText("authentication")
        .addTag("name").addText("ShiroProvider")
        .addTag("enabled").addText("true")
        .addTag( "param" )
        .addTag("name").addText("main.ldapRealm")
        .addTag("value").addText("org.apache.hadoop.gateway.shirorealm.KnoxLdapRealm").gotoParent()
        .addTag( "param" )
        .addTag("name").addText("main.ldapRealm.userDnTemplate")
        .addTag("value").addText("uid={0},ou=people,dc=hadoop,dc=apache,dc=org").gotoParent()
        .addTag( "param" )
        .addTag("name").addText("main.ldapRealm.contextFactory.url")
        .addTag("value").addText("ldap://localhost:" + ldapTransport.getPort()).gotoParent()
        .addTag( "param" )
        .addTag("name").addText("main.ldapRealm.contextFactory.authenticationMechanism")
        .addTag("value").addText("simple").gotoParent()
        .addTag("param")
        .addTag("name").addText("main.ldapRealm.authorizationEnabled")
        .addTag("value").addText("true").gotoParent()
        .addTag("param")
        .addTag( "name").addText( "urls./**")
        .addTag("value").addText( "authcBasic" ).gotoParent().gotoParent()
        .addTag( "provider" )
        .addTag( "role" ).addText( "identity-assertion" )
        .addTag( "enabled" ).addText( "true" )
        .addTag( "name" ).addText( "Default" ).gotoParent()
        .gotoRoot()
        .addTag( "service")
        .addTag("role").addText( "KNOX" )
        .gotoRoot();
    // System.out.println( "GATEWAY=" + xml.toString() );
    return xml;
  }

  private static XMLTag createTopology(String username, String password) {

    XMLTag xml = XMLDoc.newDocument(true)
        .addRoot("topology")
        .addTag("gateway")
        .addTag("provider")
        .addTag("role").addText("authentication")
        .addTag("name").addText("ShiroProvider")
        .addTag("enabled").addText("true")
        .addTag("param")
        .addTag("name").addText("main.ldapRealm")
        .addTag("value").addText("org.apache.hadoop.gateway.shirorealm.KnoxLdapRealm").gotoParent()
        .addTag("param" )
        .addTag("name").addText("main.ldapGroupContextFactory")
        .addTag("value").addText("org.apache.hadoop.gateway.shirorealm.KnoxLdapContextFactory").gotoParent()
        .addTag("param")
        .addTag("name").addText("main.ldapRealm.searchBase")
        .addTag("value").addText("ou=groups,dc=hadoop,dc=apache,dc=org").gotoParent()
        .addTag("param")
        .addTag("name").addText("main.ldapRealm.groupObjectClass")
        .addTag("value").addText("groupOfNames").gotoParent()
        .addTag("param")
        .addTag("name").addText("main.ldapRealm.memberAttributeValueTemplate")
        .addTag("value").addText("uid={0},ou=people,dc=hadoop,dc=apache,dc=org").gotoParent()
        .addTag("param" )
        .addTag("name").addText("main.ldapRealm.memberAttribute")
        .addTag("value").addText("member").gotoParent()
        .addTag("param")
        .addTag("name").addText("main.ldapRealm.authorizationEnabled")
        .addTag("value").addText("true").gotoParent()
        .addTag("param")
        .addTag("name").addText("main.ldapRealm.contextFactory.systemUsername")
        .addTag("value").addText("uid=" + username + ",ou=people,dc=hadoop,dc=apache,dc=org").gotoParent()
        .addTag("param")
        .addTag("name").addText("main.ldapRealm.contextFactory.systemPassword")
        .addTag( "value").addText(password).gotoParent()
        .addTag("param")
        .addTag("name").addText("main.ldapRealm.userDnTemplate")
        .addTag("value").addText("uid={0},ou=people,dc=hadoop,dc=apache,dc=org").gotoParent()
        .addTag("param")
        .addTag("name").addText("main.ldapRealm.contextFactory.url")
        .addTag("value").addText("ldap://localhost:" + ldapTransport.getPort()).gotoParent()
        .addTag("param")
        .addTag("name").addText("main.ldapRealm.contextFactory.authenticationMechanism")
        .addTag("value").addText("simple").gotoParent()
        .addTag("param")
        .addTag("name" ).addText("urls./**")
        .addTag("value").addText("authcBasic").gotoParent().gotoParent()
        .addTag("provider" )
        .addTag("role").addText( "identity-assertion" )
        .addTag( "enabled").addText( "true" )
        .addTag("name").addText( "Default" ).gotoParent()
        .gotoRoot()
        .addTag( "service" )
        .addTag( "role" ).addText( "test-service-role" )
        .gotoRoot();
    // System.out.println( "GATEWAY=" + xml.toString() );
    return xml;
  }

  @Test
  public void testLDAPAuth() throws Exception {

//    Test 1: Make sure authentication is successful
    outContent.reset();
    String args[] = { "system-user-auth-test", "--master", "knox", "--cluster", "test-cluster-1", "--d" };
    KnoxCLI cli = new KnoxCLI();
    cli.setConf(config);
    cli.run(args);
    assertThat(outContent.toString(), containsString("System LDAP Bind successful"));

    //    Test 2: Make sure authentication fails
    outContent.reset();
    String args2[] = { "system-user-auth-test", "--master", "knox", "--cluster", "test-cluster-2", "--d" };
    cli = new KnoxCLI();
    cli.setConf(config);
    cli.run(args2);
    assertThat(outContent.toString(), containsString("System LDAP Bind successful"));


    //    Test 3: Make sure authentication is successful
    outContent.reset();
    String args3[] = { "system-user-auth-test", "--master", "knox", "--cluster", "test-cluster-3", "--d" };
    cli = new KnoxCLI();
    cli.setConf(config);
    cli.run(args3);
    assertThat(outContent.toString(), containsString("LDAP authentication failed"));
    assertThat(outContent.toString(), containsString("Unable to successfully bind to LDAP server with topology credentials"));

    //    Test 4: Assert that we get a username/password not present error is printed
    outContent.reset();
    String args4[] = { "system-user-auth-test", "--master", "knox", "--cluster", "test-cluster-4" };
    cli = new KnoxCLI();
    cli.setConf(config);
    cli.run(args4);
    assertThat(outContent.toString(), containsString("Warn: main.ldapRealm.contextFactory.systemUsername is not present"));
    assertThat(outContent.toString(), containsString("Warn: main.ldapRealm.contextFactory.systemPassword is not present"));


    //    Test 5: Assert that we get a username/password not present error is printed
    outContent.reset();
    String args5[] = { "system-user-auth-test", "--master", "knox", "--cluster", "not-a-cluster" };
    cli = new KnoxCLI();
    cli.setConf(config);
    cli.run(args5);
    assertThat(outContent.toString(), containsString("Topology not-a-cluster does not exist"));


  }


}
