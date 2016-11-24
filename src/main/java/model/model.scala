package model
import org.apache.spark.mllib.linalg.{SparseVector => SV}
import breeze.numerics.pow
import doctor.doctorData
import org.apache.spark.{SparkConf, SparkContext}
import pre.extract.{FeedbackFile, OrderFile}
import pre.sen2vec.{segment, word2vec}
import org.apache.spark.mllib.feature.{HashingTF, IDF}
import org.apache.spark.rdd.RDD
import org.apache.spark.mllib.recommendation.Rating

import scala.collection.mutable.ArrayBuffer
object model {
  def main(args: Array[String]) {
    val orderpath="/home/lihang/mycode/mydata/hh-sql/csv/o_order.csv"
    val sc=new SparkContext(new SparkConf())
    val orderFile=new OrderFile(sc,orderpath)  //从o_order.csv中筛选数据
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
    /*下面是示例，输入一个seq，得出对应的相似的订单编号组*/
    /*val test1=textSeqData.filter{case (key,value)=>key=="O2015092109071344861"}.first()._2.toSeq
    getsim(test1).sortByKey(false).take(20).foreach(println)*/
    /*导入训练好的word2vec模型,这一部分目前没有想到好的利用办法*/
    val wordvec=new word2vec(sc)
    val model=wordvec.Model
    /*负责将一个List[String]转化为List[List[Float]]*/
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
    val textlistDataVec=textlistData.mapValues(getvec)
    //到这里rdd的内容为:订单ID->List[List[Float]],List[Float]为每个词的词向量
    // textlistDataVec.take(10).foreach(println)
    /*创建doctor对象，获取doctor数据*/
    val dc=new doctorData(sc)
    val dcData=dc.getdata()
    val dcListProSData = dcData.map(list => (list(0) -> List(list(1), list(2), list(3),list(4),list(5),list(6)))).
      mapValues(list => list(0) + " " + list(1) + " " + list(2)+" "+ list(3)+" "+list(4)+" "+list(5)).
      mapValues(seg.getwordlist)
    println(dcListProSData.first())
    /*创建feedback对象，获取feedback数据*/
    val fb=new FeedbackFile(sc)
    val fbData=fb.getdata()
    val fbData1=fbData.map(list=>List(list(2),list(0),list(3))).filter(list=>(list(0)!="NULL"&&list(1)!="NULL"&&list(2)!="NULL")).
      map(list=>(list(0),(list(1),list(2))))//订单号，用户，评分
    //fbData1.take(10).foreach(println)
    val orderex=orderData.map(list=>List(list(0),list(6))).filter(list=>(list(0)!="NULL"&&list(1)!="NULL")).
      map(list=>(list(0),list(1)))//订单号，专家
    val ratData=fbData1.join(orderex)
    val ratingDataTemp=ratData.map{
      case (key:String,ur:((String,String),String))=>{
        (ur._1._1,ur._2,ur._1._2)
      }
    }
    ratingDataTemp.take(10).foreach(println)
    val doctorid=dc.getid().map{
      case (s,i)=>
        (s,i)}.collect().toMap

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
      //  Rating(user.toInt,expert.toInt,rate.toDouble)
    }

    println(ratingData.count())
    ratingData.take(10).foreach(println)

    /*这段代码是每两个文档的相似度
    val docSims = key_idf_pairs.flatMap {
      case (id1, idf1) =>
        val idfs = b_key_idf_pairs.value.filter(_._1 != id1)
        val sv1 = idf1.asInstanceOf[SV]
        import breeze.linalg._
        val bsv1 = new SparseVector[Double](sv1.indices, sv1.values, sv1.size)
        idfs.map {
          case (id2, idf2) =>
            val sv2 = idf2.asInstanceOf[SV]
            val bsv2 = new SparseVector[Double](sv2.indices, sv2.values, sv2.size)
            val cosSim = bsv1.dot(bsv2).asInstanceOf[Double] / (norm(bsv1) * norm(bsv2))
            (id1, id2, cosSim)
        }
    }*/
    /*这段代码是使用ansj来直接获取一段文本的tfidf中的关键词
    val kwc=new KeyWordComputer(5)
    val title = "维基解密否认斯诺登接受委内瑞拉庇护"
    val content = "有俄罗斯国会议员，9号在社交网站推特表示，美国中情局前雇员斯诺登，已经接受委内瑞拉的庇护，不过推文在发布几分钟后随即删除。俄罗斯当局拒绝发表评论，而一直协助斯诺登的维基解密否认他将投靠委内瑞拉。　　俄罗斯国会国际事务委员会主席普什科夫，在个人推特率先披露斯诺登已接受委内瑞拉的庇护建议，令外界以为斯诺登的动向终于有新进展。　　不过推文在几分钟内旋即被删除，普什科夫澄清他是看到俄罗斯国营电视台的新闻才这样说，而电视台已经作出否认，称普什科夫是误解了新闻内容。　　委内瑞拉驻莫斯科大使馆、俄罗斯总统府发言人、以及外交部都拒绝发表评论。而维基解密就否认斯诺登已正式接受委内瑞拉的庇护，说会在适当时间公布有关决定。　　斯诺登相信目前还在莫斯科谢列梅捷沃机场，已滞留两个多星期。他早前向约20个国家提交庇护申请，委内瑞拉、尼加拉瓜和玻利维亚，先后表示答应，不过斯诺登还没作出决定。　　而另一场外交风波，玻利维亚总统莫拉莱斯的专机上星期被欧洲多国以怀疑斯诺登在机上为由拒绝过境事件，涉事国家之一的西班牙突然转口风，外长马加略]号表示愿意就任何误解致歉，但强调当时当局没有关闭领空或不许专机降落。";
    val content1="慢性扁桃体炎常有急性扁桃体炎反复发作的病史。发作时咽痛明显，发作间隙可以有咽干、发痒、异物感、刺激性咳嗽等轻微症状，也可能出现[口臭]、呼吸不畅、吞咽或言语障碍。扁桃体隐窝内的脓栓被咽下，刺激胃肠道，毒素被吸收，还可以出现消化不良或头痛、乏力、低热等全身反应。"
    val result = kwc.computeArticleTfidf(content)
    val result1 = kwc.computeArticleTfidf(content1)
    println(result)
    println(result1)
    */

    /*
    val rawRatings=rawData.map(_.split("::").take(3))
    val ratings=rawRatings.map{case Array(user,movie,rating)=>Rating(user.toInt,movie.toInt,rating.toDouble)}
    val model=ALS.train(ratings,50,10,0.01)
    val usersProducts=ratings.map{case Rating(user,product,rating)=>(user,product)}
    val predictions=model.predict(usersProducts).map{case Rating(user,product,rating)=>((user,product),rating)}
    val ratingsANdPredictions=ratings.map{case Rating(user,product,rating)=>((user,product),rating)}.join(predictions)
    val predictedAndTrue=ratingsANdPredictions.
      map{case ((user,product),(predicted,actual))=>(predicted,actual)}
    val regressionMetrics=new RegressionMetrics(predictedAndTrue)
    println(regressionMetrics.meanSquaredError)
    println(regressionMetrics.rootMeanSquaredError)
    val itemFactors=model.productFeatures.map{case (id,factor)=>factor}.collect()
    val s=new DoubleMatrix(Array(1.0,2.0,3.0))
    val itemMatrix=new DoubleMatrix(itemFactors)
    val imBroadcast=sc.broadcast(itemMatrix)
    val allRecs=model.userFeatures.map{case (userId,array)=>
    val userVector=new DoubleMatrix(array)
    val scores=imBroadcast.value.mmul(userVector)
    val sortedWithId=scores.data.zipWithIndex.sortBy(-_._1)
    val recommendedIds=sortedWithId.map(_._2+1).toSeq
      (userId,recommendedIds)}
    val userMovies=ratings.map{case Rating(user,product,rating)=>
      (user,product)}.groupBy(_._1)
    val predictedAndTrueForRanking=allRecs.join(userMovies).map{case (userId,(predicted,actualWithIds))=>
    val actual=actualWithIds.map(_._2)
      (predicted.toArray,actual.toArray)}
    val rankingMetrics=new RankingMetrics(predictedAndTrueForRanking)
    println(rankingMetrics.meanAveragePrecision)
    */
    /*
    val MAPK2000=allRecs.join(userMovies).map{case(userId,(predicted,actualWithIds))=>
    val actual=actualWithIds.map(_._2).toSeq
    avgPrecisionK(actual,predicted,2000)}.reduce(_+_)/allRecs.count()
    println(MAPK2000)
    */
    //val predictedRating=model.predict(65314,3252)
    //println(predictedRating)
    // Build the recommendation model using ALS on the training
  }
}
