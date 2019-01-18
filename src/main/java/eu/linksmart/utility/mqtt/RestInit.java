package eu.linksmart.utility.mqtt;

import eu.almanac.event.datafusion.utils.generic.ComponentInfo;
import eu.linksmart.services.utils.EditableService;
import eu.linksmart.services.utils.configuration.Configurator;
import eu.linksmart.services.utils.function.Utils;
import eu.linksmart.services.utils.serialization.*;
import io.swagger.client.ApiClient;
import io.swagger.client.api.ScApi;
import io.swagger.client.model.Service;
import io.swagger.client.model.ServiceDocs;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.Contact;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

import java.io.IOException;
import java.net.InetAddress;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by José Ángel Carvajal on 10.04.2017 a researcher of Fraunhofer FIT.
 */
@Configuration
@PropertySource("__def__conf__.cfg")
@EnableAutoConfiguration
@ConfigurationProperties
@SpringBootApplication
@RestController
@EnableSwagger2
public class RestInit {
    static Properties info = null;
    protected transient static Logger loggerService = LogManager.getLogger(RestInit.class);
    private static Configurator conf = Configurator.getDefaultConfig();
    private static boolean la = false;
    static final String SPRING_MANAGED_FEATURES = "spring_managed_configuration_features", CATALOG_ENDPOINT= "linksmart_service_catalog_endpoint", HOST_NAME= "linksmart_service_dns_hostname";
    private static boolean gpl = false;

    private static SerializerDeserializer serializer = new DefaultSerializerDeserializer();
    private static String MPS_ID = "linksmart_mps_id";

    public static void init() {

        try {
            Class.forName(Rest2Mqtt.class.getCanonicalName());
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        try {
            info = Utils.createPropertyFiles("service.info");
        } catch (IOException e) {
            LogManager.getLogger(RestInit.class).error(e.getMessage(), e);
        }



        SpringApplication springApp;

        springApp = new SpringApplication(RestInit.class);

        springApp.setDefaultProperties(toProperties(conf));

        springApp.addInitializers();
        springApp.run();
        if(conf.containsKeyAnywhere(CATALOG_ENDPOINT)){
            try {
                String host = conf.containsKeyAnywhere(HOST_NAME) ? conf.getString(HOST_NAME) : InetAddress.getLocalHost().getHostName();
                ApiClient client = new ApiClient();
                client.setBasePath(conf.getString(CATALOG_ENDPOINT));

                ScApi SCclient = new ScApi(client);
                EditableService myRegistration = new EditableService();

                myRegistration.setDescription("LinkSmart(R) Multicaster tunneler");
                myRegistration.setName("_linksmart-mps.tcp_");
                myRegistration.setId(conf.containsKeyAnywhere(MPS_ID)?conf.getString(MPS_ID):host);

                myRegistration.setApis(new ConcurrentHashMap<>());
                myRegistration.setDocs(new ArrayList<>());
                myRegistration.setTtl(86400L);

                String port = conf.containsKeyAnywhere("server_port") ?conf.getString("server_port"):"8312", protocol = "http";
                if (conf.containsKeyAnywhere("server_ssl_key-store"))
                    protocol = "https";

                myRegistration.getApis().put("rest2mqtt", protocol + "://" + host + ":" + port + "/rest/");
                myRegistration.getApis().put("rest2mqtt-lite", protocol + "://" + host + ":" + port + "/mqtt/");
                myRegistration.getApis().put("post2pub", protocol + "://" + host + ":" + port + "/publish/");

                ServiceDocs doc = new ServiceDocs();
                doc.setDescription("Open API V2");
                doc.setUrl(protocol + "://" + host + ":" + port + "/swagger-ui.html");
                doc.setType("openAPI");
                doc.setApis(new ArrayList<>(myRegistration.getApis().values()));
                myRegistration.setDocs(Arrays.asList(doc));

                loggerService.info("registering service as "+ serializer.toString(myRegistration));
                SCclient.idPut(myRegistration.getId(),myRegistration);


            }catch (Exception e){
                loggerService.error(e.getMessage(),e);
            }
        }


    }
    @RequestMapping(value = "/", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> status() {

        Map<String, Object> map = new Hashtable<>();
        Map<String, ArrayList<ComponentInfo>> aux = new Hashtable<>();
        map.put("Distribution", info.getProperty("linksmart.service.info.distribution.name"));


        map.put("LoadedComponents",aux);
        try {
            return new ResponseEntity<>(serializer.toString(map), HttpStatus.OK);
        }catch (Exception e){
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }

    }
    @Bean
    public Docket api() {
        return new Docket(DocumentationType.SWAGGER_2)
                .groupName(info.getProperty("linksmart.service.info.distribution.name"))
                .apiInfo(apiInfo())
                .select()
                .apis(RequestHandlerSelectors.any())
                .paths(PathSelectors.any())
                .build();
    }
    private ApiInfo apiInfo() {
        return new ApiInfoBuilder()
                .title(info.getProperty("linksmart.service.info.distribution.name"))
                .description(info.getProperty("linksmart.service.info.distribution.description"))
                .version(Utils.getVersion())
                        //.termsOfServiceUrl("http://www-03.ibm.com/software/sla/sladb.nsf/sla/bm?Open")
                .contact(new Contact(info.getProperty("linksmart.service.info.distribution.name"),info.getProperty("linksmart.service.info.distribution.contact.url"),info.getProperty("linksmart.service.info.distribution.contact.email")))
                .license("linksmart.service.info.distribution.license")
                .licenseUrl("linksmart.service.info.distribution.url")
                .build();
    }
    static private Properties toProperties(Configurator configurator){
        Properties properties = new Properties();
        if(configurator.containsKeyAnywhere(SPRING_MANAGED_FEATURES)) {
            String [] keys = configurator.getStringArray(SPRING_MANAGED_FEATURES);
            for (String key :keys) {
                if (configurator.containsKeyAnywhere(key))
                    properties.put(key, configurator.getString(key));
            }
        }
        return properties;
    }

}
