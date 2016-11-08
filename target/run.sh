#!/usr/bin/env bash
#num-workers ´ú±íÊ¹ÓÃµÄ½ÚµãÊý ×î¶à5¸ö
#worker-cores ´ú±íÃ¿¸ö½ÚµãµÄCPUºËÊý ×î¶à12¸ö
#worker-memory ½ÚµãÊ¹ÓÃµÄÄÚ´æ×î¶à100000m

#export YARN_CONF_DIR=/opt/hadoop-2.2.0/etc/hadoop

#$SPARK_HOME/bin/spark-submit \
#--class mfby \
#--master spark://dell01:7077 \
#--jars spark-indexedrdd_2.11-0.1-SNAPSHOT.jar \
#--executor-memory 8g \
#--total-executor-cores 48 \
#recommendation-1.0-SNAPSHOT.jar \
spark-submit --class ALSExample recommendation-1.0-SNAPSHOT.jar