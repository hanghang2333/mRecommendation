package model
import org.apache.spark.mllib.linalg.{SparseVector => SV}
import breeze.numerics.pow
import doctor.doctorData
import org.apache.spark.{SparkConf, SparkContext}
import pre.extract.{FeedbackFile, OrderFile}
import pre.sen2vec.{segment, word2vec}
import org.apache.spark.mllib.feature.{HashingTF, IDF}
import org.apache.spark.rdd.RDD
import org.apache.spark.mllib.recommendation.{ALS, Rating}

import scala.collection.mutable.ArrayBuffer

class W2Vmodel(@transient sc:SparkContext)extends Serializable{
  @transient
  val wordvec=new word2vec(sc)
  val model=wordvec.Model
  def getvec(str:List[String]):List[List[Float]]= {
    val ab = new ArrayBuffer[List[Float]]()
    for (s <- str) {
      try {
        ab+=model.getVectors(s).toList
      }
      catch {
        case ex: NoSuchElementException => {
        }
      }
    }
    ab.toList
  }
  /*输入RDD[(String,List[String])]，输出RDD[(String,List[List[Float]])]*/
  def getwordvec(data:RDD[(String,List[String])]):RDD[(String,List[List[Float]])]={
    val textlistDataVec=data.mapValues(getvec)
    textlistDataVec
  }

}