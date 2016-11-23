package pre.sen2vec
import org.apache.spark.{SparkConf, SparkContext}
import org.apache.spark.mllib.feature.{Word2Vec, Word2VecModel}
class word2vec(@transient sc:SparkContext){
    val modelpath="/home/lihang/mycode/mydata/wordvec"
    val Model = Word2VecModel.load(sc, modelpath)
/*
import org.apache.spark.{SparkConf, SparkContext}
import org.apache.spark.mllib.feature.{Word2Vec, Word2VecModel}
object word2vecmodel{
  def main(args:Array[String])={
    val conf=new SparkConf()
    val sc=new SparkContext(conf)
    val input = sc.textFile("/home/lihang/mycode/mydata/text/text_now").map(line => line.split(" ").toSeq)
    val word2vec = new Word2Vec().setVectorSize(100)
    val model = word2vec.fit(input)
    val synonyms = model.findSynonyms("高血压", 5)
    // Save and load model
    for(s:(String,Double) <- synonyms){
        println(s)
    } //println(synonyms)
    model.save(sc, "/home/lihang/mycode/mydata/wordvec/")
    //val sameModel = Word2VecModel.load(sc, "myModelPath")
  }
}

 */
}