package cdmq.example;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.serialization.StringSerializer;

public class ProducerExample {

    public static void main(String[] args) throws InterruptedException, ExecutionException {
        // 接入点有两种类型，一是域名，二是CL5。域名仅支持原MIG范围内的机器，是过渡办法，CL5是未来趋势，后续CDMQ也会统一使用CL5来做服务发现
        String entry = "cdmqszentry01.data.mig:10005,cdmqszentry02.data.mig:10069";// 接入点，在管理页面中可查询到。
        String clientId = "p_test_example_usage";// 生产者id，在管理页面自行创建，并关联上topic。必填，否则无法访问到CDMQ
        String topic = "U_TOPIC_example_usage"; // 在管理页面中创建

        if (args.length == 3) {
            entry = args[0];
            clientId = args[1];
            topic = args[2];
        }

        Map<String, Object> prop = new HashMap<>();
        prop.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, entry);
        prop.put(ProducerConfig.CLIENT_ID_CONFIG, clientId);
        prop.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, 3000); // 请求在服务端最长请求处理时间
        prop.put(ProducerConfig.LINGER_MS_CONFIG, 1000); // 数据最长在内存中驻留多久就发往服务端   根据数据量的情况酌情设置，保证能够利用到Kafka的批量发包特性，以将Kafka的性能发挥到最大化
        prop.put(ProducerConfig.BATCH_SIZE_CONFIG, 65535); // 数据在组包过程中，batch最大大小。一个batch中包含多条消息。详细参考kafka的请求格式。
        prop.put(ProducerConfig.MAX_REQUEST_SIZE_CONFIG, 131072); // 一次请求最大大小。一次请求中包含多个batch。为了保证消息的读写质量和最优化Kafka的性能，CDMQ平台限制单次最大大小为128K。
        prop.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "gzip"); // 请酌情启用数据压缩。经过压缩的数据，在MQ的处理过程中吞吐能力更强，成本更优
        KafkaProducer<String, String> producer = new KafkaProducer<>(prop, new StringSerializer(), new StringSerializer());

        System.out.println("start send msg");
        producer.send(new ProducerRecord<String, String>(topic, "" + System.currentTimeMillis(), "msgcontent"), new Callback() {

            @Override
            public void onCompletion(RecordMetadata metadata, Exception exception) {
                if (exception == null) {
                    System.out.println("sendMsgSucc," + metadata.toString());
                } else {
                    System.err.println("sendMsgFail," + exception.getMessage());
                }
            }
        }).get();
        System.out.println("send over");
        producer.close();
    }

}
