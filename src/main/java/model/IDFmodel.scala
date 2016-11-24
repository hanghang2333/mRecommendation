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
class IDFmodel(@transient sc:SparkContext)extends Serializable{
  @transient
  val orderFile=new OrderFile(sc,"/home/lihang/mycode/mydata/hh-sql/csv/o_order.csv")  //从o_order.csv中筛选数据
  val orderData=orderFile.getdata()//0->订单编号 1->用户编号 2->会诊目的 3->患者医院ID 4->诊断描述 5->疾病方向 6->接单专家ID
  val textData=orderData.map(list=>List(list(0),list(2),list(4),list(5)))//订单编号,会诊目的,诊断描述,疾病方向
  /*导入分词类*/
  val seg=new segment()
  val textlistData=textData.map(list=>(list(0)->List(list(1),list(2),list(3)))).
    mapValues(list=>list(0)+" "+list(1)+" "+list(2)).
    mapValues(seg.getwordlist)
  //到这里rdd里存储的内容为 订单编号+List[String]
  //下面是开始训练IFTDF的过程
  val textSeqData=textlistData.mapValues(list=>list.toSeq)
  val hashingTF = new HashingTF(pow(2,18).toInt)
  val key_tf_pairs = textSeqData.map {
    case (key,text) =>
      val tf = hashingTF.transform(text)
      (key,tf)   }
  key_tf_pairs.cache() //构建idf model
  val idf = new IDF().fit(key_tf_pairs.values) //将tf向量转换成tf-idf向量
  val key_idf_pairs = key_tf_pairs.mapValues(v =>idf.transform(v)) //广播一份tf-idf向量集
  val b_key_idf_pairs = sc.broadcast(key_idf_pairs.collect()) //计算doc之间余弦相似度
  def getsim(seqtext:Seq[String]):RDD[(Double,String)]={
    val tftest=hashingTF.transform(seqtext)
    val idftest=idf.transform(tftest)
    val simtest={
      val svtest=idftest.asInstanceOf[SV]
      import breeze.linalg._
      val bsvtest=new SparseVector[Double](svtest.indices,svtest.values,svtest.size)
      b_key_idf_pairs.value.map{
        case (id3, idf3) =>
          val sv3 = idf3.asInstanceOf[SV]
          val bsv3 = new SparseVector[Double](sv3.indices, sv3.values, sv3.size)
          val cosSim = bsvtest.dot(bsv3).asInstanceOf[Double] / (norm(bsvtest) * norm(bsv3))
          (cosSim,id3)
      }}
    val simrdd=sc.parallelize(simtest)
    simrdd
  }
}