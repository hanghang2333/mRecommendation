package pre.sen2vec
import org.apache.spark.mllib.linalg.{SparseVector => SV}
import breeze.numerics.pow
import org.apache.spark.{SparkConf, SparkContext}
import org.apache.spark.mllib.feature.{HashingTF, IDF}
import org.apache.spark.rdd.RDD

class  tfidf(@transient sc:SparkContext,textSeqData:RDD[Map[String,Seq[String]]]){

}