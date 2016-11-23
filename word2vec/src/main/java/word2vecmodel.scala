import org.apache.spark.{SparkConf, SparkContext}
import org.apache.spark.mllib.feature.{Word2Vec, Word2VecModel}
object word2vecmodel{
  def main(args:Array[String])={
    val conf=new SparkConf()
    val sc=new SparkContext(conf)
    val input = sc.textFile("/home/lihang/mycode/mydata/text/text_now").map(line => line.split(" ").toSeq)
    val word2vec = new Word2Vec().setVectorSize(100)
    val model = word2vec.fit(input)
    val synonyms = model.findSynonyms("医生", 5)
    // Save and load model
    for(s:(String,Double) <- synonyms){
        println(s)
    } //println(synonyms)
    //val res1=model.getVectors("高血压")._ 2
    //println(res1)
    model.save(sc, "/home/lihang/mycode/mydata/wordvec/")
    //val sameModel = Word2VecModel.load(sc, "myModelPath")
  }
}
