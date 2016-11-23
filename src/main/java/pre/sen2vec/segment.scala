package pre.sen2vec
import java.util

import collection.JavaConversions._
import org.nlpcn.commons.lang.tire.domain.Forest
import org.ansj.domain.{Result, Term}
import org.ansj.splitWord.analysis.ToAnalysis
import org.ansj.library.UserDefineLibrary
import org.ansj.library.UserDefineLibrary
import org.ansj.recognition.impl.FilterRecognition
import org.nlpcn.commons.lang.tire.library.Library

import scala.io.Source
import scala.collection.mutable.ArrayBuffer
class segment extends Serializable{
  //导入停止词
  @transient
  val stopword=ArrayBuffer[String]()
  for(line<-Source.fromFile("/home/lihang/mycode/mydata/stop/stop").getLines()){
    stopword+=line.toString
  }
  val stopwordlist=stopword.toList
  /*输入:string输出List[String]*/
  def getwordlist(str:String):List[String]={
    val fitler = new FilterRecognition()
    fitler.insertStopNatures("w","r","c"); //过滤词性
    fitler.insertStopWords(stopwordlist); //过滤单词
    //fitler.insertStopRegex("小.*?"); //支持正则表达式
    val b = ArrayBuffer[String]()
    for(term:Term <- ToAnalysis.parse(str).recognition(fitler)){
       b+=term.getName
    }
    b.toList
  }
  //def gettfidf(str:String):List[String]={
//}
}
/*
val text = sc.textFile("/path/to/ChineseFile", numpatitions).map { x =>
val temp = ToAnalysis.parse(x)
//加入停用词
FilterModifWord.insertStopWords(Arrays.asList("r","n"))
//加入停用词性
FilterModifWord.insertStopNatures("w",null,"ns","r","u","e")
val filter = FilterModifWord.modifResult(temp)
//此步骤将会只取分词，不附带词性
val word = for(i<-Range(0,filter.size())) yield filter.get(i).getName
word.mkString("\t")
    }
  text.saveAsTextFile("/pathr/to/TokenFile")
}
*/