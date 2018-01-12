package eu.linksmart.utility.mqtt;


import eu.linksmart.api.event.types.Statement;
import eu.linksmart.api.event.types.impl.MultiResourceResponses;
import eu.linksmart.services.event.types.StatementInstance;
import eu.linksmart.services.utils.serialization.DefaultSerializer;
import eu.linksmart.services.utils.serialization.Serializer;

import java.util.Collections;

import static org.junit.Assert.fail;

/**
 * Created by José Ángel Carvajal on 07.12.2017 a researcher of Fraunhofer FIT.
 */
public class MqttRequestManagerTest {
    //@Test
    public void broker(){
        try {
            MqttRequestManager requestManager = new MqttRequestManager();
            Serializer serializer = new DefaultSerializer();
            Statement statement = new StatementInstance(
                    "test",
                    "select count(*) as count from Observation output every 1 sec",
                    Collections.singletonList("local")
            );

            MultiResourceResponses responses = requestManager.request("/statement/new/test/",serializer.serialize(statement), 3,3000,null,null);
            System.out.println(serializer.toString(responses));
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }

    }
}
