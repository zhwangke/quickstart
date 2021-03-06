package cdmq.example;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;

public class ConsumerExample {

    public static void main(String[] args) {
        
        String entry = "cdmqszentry01.data.mig:10005,cdmqszentry02.data.mig:10069";// 接入点broker
        String topic = "U_TOPIC_example_usage"; // 在管理页面中创建
        String groupId = "cg_test_example_usage";// 消费组id，自行创建，并关联上topic。必填，否则无法访问CDMQ
        int msgCount = 10;

        if (args.length >= 3) {
            entry = args[0];
            topic = args[1];
            groupId = args[2];
            if (args.length == 4) {
                msgCount = Integer.parseInt(args[3]);
            }
        }

        Map<String, Object> prop = new HashMap<>();
        prop.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, entry);
        prop.put(ConsumerConfig.CLIENT_ID_CONFIG, groupId); // client.id必设，必须与group.id取同样的值，否则无法访问到CDMQ
        prop.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        prop.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, true); // 是否自动提交消费进度
        prop.put(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, 3000); // 当enable.auto.commit为true时，定时多久一次提交消费进度
        prop.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest"); // 当使用服务端来保存消费进度时，如果服务端没有group.id这个消费组的消费进度时，latest直接消费kafka最新数据  earliest直接消费kafka最旧数据 none抛异常

        prop.put(ConsumerConfig.FETCH_MAX_BYTES_CONFIG, 1048576); // 单次消费拉取请求，最大拉取包大小。
        // 根据经验，这里设置100K-1M比较合适。设置太大，消费吞吐力上不去（从服务端拉数据和业务处理数据差不多变串行了），设置太小，则sdk会发送大量请求到服务端，kafka服务端TPS很容易触达上限，导致整体吞吐能力下降
        // 不论值大与小，kafka服务端如果有数据返回，一定会返回一个完整的数据包。（比如一条消息大小为1K，设置fetch.max.bytes为900，则也会返回这一条1K大消息的完整包）
        prop.put(ConsumerConfig.FETCH_MAX_WAIT_MS_CONFIG, 1000); // 单次消费拉取请求最长等待时间。最长等待时间仅在没有最新数据时才会等待。此值应当设置较大点，减少空请求对服务端QPS的消耗。

        prop.put(ConsumerConfig.MAX_PARTITION_FETCH_BYTES_CONFIG, 524288); // 单次消费拉取请求中，单个分区最大返回消息大小。一次拉取请求可能返回多个分区的数据，这里限定单个分区的最大数据大小

        KafkaConsumer<String, String> kafkaConsumer = new KafkaConsumer<>(prop, new StringDeserializer(), new StringDeserializer());

        kafkaConsumer.subscribe(Collections.singletonList(topic));

        try {
            while (msgCount > 0) {
                ConsumerRecords<String, String> records = kafkaConsumer.poll(1000);
                for (ConsumerRecord<String, String> record : records) {
                    System.out.println("recvMsg," + record.toString());
                    msgCount--;
                }
            }
            kafkaConsumer.close();
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }

}
