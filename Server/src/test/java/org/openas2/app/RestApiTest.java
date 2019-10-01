package org.openas2.app;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.http.HttpEntity;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.openas2.TestResource;

import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertThat;



@RunWith(MockitoJUnitRunner.class)
/**
 *
 * @author javier
 */
public class RestApiTest {
    private static final TestResource RESOURCE = TestResource.forGroup("SingleServerTest");
    // private static File openAS2AHome;
    private static OpenAS2Server serverInstance;
    private static ExecutorService executorService;
    private static TemporaryFolder scratchpad = new TemporaryFolder();
    private static CloseableHttpClient httpclient;

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    @BeforeClass
    public static void startServer() throws Exception {
	// Set up some override properties so we can use the standard config in tests
	// to make sure the release package is fully tested
	scratchpad.create();
	File customPropsFile = scratchpad.newFile("openas2.properties");
	System.setProperty("openas2.properties.file", customPropsFile.getAbsolutePath());
	FileOutputStream fos = new FileOutputStream(customPropsFile);
	fos.write("restapi.command.processor.enabled=true\n".getBytes());
	fos.close();

	try {
	    System.setProperty("org.apache.commons.logging.Log", "org.openas2.logging.Log");
	     System.setProperty("org.openas2.logging.defaultlog", "TRACE");
//	    executorService = Executors.newFixedThreadPool(20);

	    RestApiTest.serverInstance = new OpenAS2Server.Builder()
		    .run(RESOURCE.get("MyCompany", "config", "config.xml").getAbsolutePath());
	} catch (Throwable e) {
	    // aid for debugging JUnit tests
	    System.err.println("ERROR occurred: " + ExceptionUtils.getStackTrace(e));
	    throw new Exception(e);
	}
        
    }
    
    @BeforeClass
    public static void startClient() throws Exception {      
        RestApiTest.httpclient = HttpClients.createDefault();
    }

    @AfterClass
    public static void tearDown() throws Exception {
	serverInstance.shutdown();
//	executorService.shutdown();
	System.clearProperty("openas2.properties.file");
	scratchpad.delete();
        httpclient.close();
    }

    protected String doRequest(HttpRequestBase request,boolean withAuth) throws IOException {
        String buffer="";
        HttpClientContext context = HttpClientContext.create();
        if(withAuth) {
            UsernamePasswordCredentials creds = new UsernamePasswordCredentials("userID", "pWd");
            CredentialsProvider credsProvider = new BasicCredentialsProvider();
            credsProvider.setCredentials(AuthScope.ANY, creds);
            context.setCredentialsProvider(credsProvider);
        }
        CloseableHttpResponse response = RestApiTest.httpclient.execute(request,context);
        try {
            
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                long len = entity.getContentLength();
                if (len != -1 && len < 2048) {
                    buffer=EntityUtils.toString(entity);
                } else {
                    throw new RuntimeException("Response too long");
                }
            }
        } finally {
            response.close();
        }
        return buffer;
    }
    @Test
    public void shouldRespondWithVersion() throws Exception {
        String buffer=this.doRequest(new HttpGet("http://127.0.0.1:8080/api/"), false);
        assertThat("Getting API version and server info", buffer, containsString(serverInstance.getSession().getAppTitle()));
        
        buffer = this.doRequest(new HttpPost("http://127.0.0.1:8080/api/partner/list"),false);
        assertThat("Getting API without user/pass", buffer, containsString("You cannot access this resource"));
        
        buffer = this.doRequest(new HttpPost("http://127.0.0.1:8080/api/partnership/list"),false);
        assertThat("Getting API without user/pass", buffer, containsString("You cannot access this resource"));
        
        buffer = this.doRequest(new HttpPost("http://127.0.0.1:8080/api/cert/list"),false);
        assertThat("Getting API without user/pass", buffer, containsString("You cannot access this resource"));
        
//        buffer = this.doRequest(new HttpGet("http://127.0.0.1:8080/api/partner/list"),true);
//        assertThat("Getting Partners API ", buffer, containsString("\"type\":\"OK\""));
//        
//        buffer = this.doRequest(new HttpGet("http://127.0.0.1:8080/api/partnership/list"),true);
//        assertThat("Getting Partnership API ", buffer, containsString("\"type\":\"OK\""));
//        
//        buffer = this.doRequest(new HttpGet("http://127.0.0.1:8080/api/cert/list"),true);
//        assertThat("Getting Certs API ", buffer, containsString("\"type\":\"OK\""));
        
        
    }
}
