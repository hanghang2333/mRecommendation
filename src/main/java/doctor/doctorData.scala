package doctor
import org.apache.spark.rdd.RDD
import org.apache.spark.{SparkConf, SparkContext}
import pre.sen2vec.segment

class doctorData(@transient sc:SparkContext){
  def getdata(): RDD[(String,List[String])] = {
    val dcData = sc.textFile("/home/lihang/mycode/mydata//hh-sql/csv/tdoctor.csv")
    val dcListProData = dcData.map(x => x.split(",")).filter(list => list.length == 43).filter(list => (list(31) != "-1" && list(33) == "0"))
    val dcListNorData = dcData.map(x => x.split(",")).filter(list => list.length == 43).filter(list => (list(31) == "-1" && list(33) == "0"))
    val dcListProEData = dcListProData.map(list => List(list(0), list(11), list(19), list(20), list(23), list(27), list(28)))
    /*以上筛选的部分为 0-编号 1-专长 2-科室名称 3-标准科室 4-专家简介 5-子方向 6-擅长疾病*/
    val seg = new segment() /*导入分词类分词*/
    val dcListProSData = dcListProEData.map(list => (list(0) -> List(list(1), list(2), list(3)))).
      mapValues(list => list(0) + " " + list(1) + " " + list(2)).
      mapValues(seg.getwordlist)
    dcListProSData
  }
}