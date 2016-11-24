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
class CFmodel(@transient sc:SparkContext){
  def getrating():(RDD[Rating],Map[String,Int])={
    /*订单记录，从这里面获取订单号到医生号和专家号的映射*/
    val orderpath="/home/lihang/mycode/mydata/hh-sql/csv/o_order.csv"
    val orderFile=new OrderFile(sc,orderpath)  //从o_order.csv中筛选数据
    val orderData=orderFile.getdata()//0->订单编号 1->用户编号 2->会诊目的 3->患者医院ID 4->诊断描述 5->疾病方向 6->接单专家ID
    /*反馈数据，从其中获取订单和评分的映射*/
    val fb=new FeedbackFile(sc)
    val fbData=fb.getdata()
    val fbData1=fbData.map(list=>List(list(2),list(0),list(3))).filter(list=>(list(0)!="NULL"&&list(1)!="NULL"&&list(2)!="NULL")).
      map(list=>(list(0),(list(1),list(2))))//订单号，用户，评分
    /*获取医生信息，主要是建立医生编号到简单Int值之间的映射，因为编号太长无法组成Rating数据*/
    val dc=new doctorData(sc)
    val doctorid=dc.getid().map{
      case (s,i)=>
        (s,i)}.collect().toMap
    /*从上面的数据里依次提取信息*/
    val orderex=orderData.map(list=>List(list(0),list(6))).filter(list=>(list(0)!="NULL"&&list(1)!="NULL")).
      map(list=>(list(0),list(1)))//订单号，专家
    val ratData=fbData1.join(orderex)
    val ratingDataTemp=ratData.map{
      case (key:String,ur:((String,String),String))=>{
        (ur._1._1,ur._2,ur._1._2)
      }
    }
    val ratingDataTemp2=ratingDataTemp.map{
      case (x1,x2,x3)=> x3 match{
        case "3"=>(doctorid.get(x1),doctorid.get(x2),5)
        case "4"=>(doctorid.get(x1),doctorid.get(x2),4)
        case "5"=>(doctorid.get(x1),doctorid.get(x2),3)
        case "6"=>(doctorid.get(x1),doctorid.get(x2),2)
        case "7"=>(doctorid.get(x1),doctorid.get(x2),1)
      }
    }
    val ratingData=ratingDataTemp2.filter{
      case(x1,x2,x3)=>{
        x1!=None&&x2!=None
      }
    }.map{
      case(Some(x1),Some(x2),x3)=> {
        (x1,x2,x3)
      }
    }
    val ratings=ratingData.map{
      case(user,expert,rate)=>
        Rating(user,expert,rate.toDouble)
    }
    (ratings,doctorid)
  }
}