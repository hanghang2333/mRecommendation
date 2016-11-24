package pre.extract
import org.apache.spark.SparkContext
import org.apache.spark._
import org.apache.spark.rdd.RDD
class FeedbackFile(@transient sc:SparkContext) {
  //输入:输出:RDD[List[String]]
  def getdata(): RDD[List[String]] = {
    val feedbackfile="/home/lihang/mycode/mydata/hh-sql/csv/tfeedback.csv"
    val FeedData = sc.textFile(feedbackfile)
    val FeedListData = FeedData.map(_.split(",")).filter(list => list.length == 18).
      filter(list=>(list(8)!="NULL"&&(list(10)=="3"||list(10)=="4"||list(10)=="5"||list(10)=="6"||list(10)=="7")&&list(9)=="2"))
    /*上面是筛选过滤，去除了没有order_id，isgood不合理,产品本身反馈*/
    val FeedDListData=FeedListData.filter(list=>list(2)=="1").map(list=>List(list(1),list(7),list(8),list(10)))
    //0->用户编号 1->反馈信息 2->orderid 3->isgood评分
    val FeedEListData=FeedListData.filter(list=>list(2)=="2").map(list=>List(list(1),list(7),list(8),list(10)))
    FeedDListData
  }
}