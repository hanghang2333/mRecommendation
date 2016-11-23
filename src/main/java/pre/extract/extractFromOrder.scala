package pre.extract
import org.apache.spark.SparkContext
import org.apache.spark._
import org.apache.spark.rdd.RDD
class OrderFile(@transient sc:SparkContext,orderfile:String) {
  //输入:输出:RDD[List[String]]
  def getdata(): RDD[List[String]] = {
      val rawData = sc.textFile(orderfile)
      val rawDataList = rawData.map(_.split(",")).filter(list => list.length == 36).
        filter(list => (list(30) != "NULL" && list(31) != "NULL") && list(25) == "0").
        map(list => List(list(0), list(2), list(11), list(12), list(30), list(31), list(35)))
    //0->订单编号 1->用户编号 2->会诊目的 3->患者医院ID 4->诊断描述 5->疾病方向 6->接单专家ID
      rawDataList
  }
}