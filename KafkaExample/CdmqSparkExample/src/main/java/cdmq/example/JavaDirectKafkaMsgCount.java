package cdmq.example;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.spark.SparkConf;
import org.apache.spark.streaming.Durations;
import org.apache.spark.streaming.api.java.JavaDStream;
import org.apache.spark.streaming.api.java.JavaInputDStream;
import org.apache.spark.streaming.api.java.JavaStreamingContext;
import org.apache.spark.streaming.kafka010.ConsumerStrategies;
import org.apache.spark.streaming.kafka010.KafkaUtils;
import org.apache.spark.streaming.kafka010.LocationStrategies;

public final class JavaDirectKafkaMsgCount {

    public static void main(String[] args) throws Exception {
        // 接入点有两种类型，一是域名，二是CL5。域名仅支持原MIG范围内的机器，是过渡办法，CL5是未来趋势，后续CDMQ也会统一使用CL5来做服务发现
        String brokers = "cdmqszentry01.data.mig:10005,cdmqszentry02.data.mig:10069";// 接入点，在管理页面中可查询到
        String topics = "U_TOPIC_example_usage";// 在管理页面中创建
        String groupId = "cg_test_example_usage"; // 消费组id，在管理页面自行创建，并关联上topic。必填，否则无法访问到CDMQ

        // Create context with a 2 seconds batch interval
        SparkConf sparkConf = new SparkConf().setAppName("JavaDirectKafkaMsgCount").setMaster("local[*]");
        JavaStreamingContext jssc = new JavaStreamingContext(sparkConf, Durations.seconds(2));

        Set<String> topicsSet = new HashSet<>(Arrays.asList(topics.split(",")));
        Map<String, Object> prop = new HashMap<>();
        prop.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, brokers);
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

        prop.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class);
        prop.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class);

        // Create direct kafka stream with brokers and topics
        JavaInputDStream<ConsumerRecord<byte[], byte[]>> messages = KafkaUtils.createDirectStream(jssc, LocationStrategies.PreferConsistent(), ConsumerStrategies.Subscribe(topicsSet, prop));

        JavaDStream<byte[]> lines = messages.map(x -> x.value()); // JDK8中不支持messages.map(ConsumerRecord::value)这种写法
        lines.count().print();

        // Start the computation
        jssc.start();
        jssc.awaitTermination();
    }
}
