#!/usr/bin/env python
#coding:utf-8

from kafka import KafkaConsumer
import time
import sys
from kafka import TopicPartition

def prn_obj(obj): 
  print '\n'.join(['%s:%s' % item for item in obj.__dict__.items()]) 

if __name__ == '__main__':
  if len(sys.argv) < 3:
    print("para topic and cid are needed!!!")
    sys.exit()
  topic=sys.argv[1]
  cid=sys.argv[2]
  try:
    DEFAULT_CONFIG = {
      'key_deserializer': None,
      'value_deserializer': None,
      'fetch_max_wait_ms': 500,
      'fetch_min_bytes': 1,
      'fetch_max_bytes': 1048576,
      'max_partition_fetch_bytes': 524288,
      'request_timeout_ms': 40 * 1000,
      'retry_backoff_ms': 100,
      'reconnect_backoff_ms': 50,
      'reconnect_backoff_max_ms': 1000,
      'max_in_flight_requests_per_connection': 5,
      'auto_offset_reset': 'earliest',
      'enable_auto_commit': True,
      'auto_commit_interval_ms': 3000,
      'metadata_max_age_ms': 5 * 60 * 1000,
      'heartbeat_interval_ms': 3000,
      'session_timeout_ms': 30000,
      'max_poll_records': 500
    }
    #必须项,目前所有虫洞的server地址都一样
    DEFAULT_CONFIG['bootstrap_servers']='cdmqszentry01.data.mig:10005,cdmqszentry02.data.mig:10069'
    #生产id,cdmq页面申请的消费者
    DEFAULT_CONFIG['client_id']=cid
    DEFAULT_CONFIG['group_id']=cid
    consumer=KafkaConsumer(topic,**DEFAULT_CONFIG)
    #consumer=KafkaConsumer(**DEFAULT_CONFIG)
    #partition = TopicPartition(topic,72)
    #consumer.assign([partition])
    #consumer.seek(partition,0)    
    print("begin to consume data")
    #其他支持的参数可以参见官方文档https://kafka-python.readthedocs.io/en/master/apidoc/KafkaConsumer.html
    for msg in consumer:
        print(msg.partition) 
      #print('topic:{} partition:{} value:{}'.format(msg.topic,msg.partition,msg.value))
      #time.sleep(5)
  except Exception as e:
    print(str(e))
