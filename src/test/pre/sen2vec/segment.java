import java.util.List;

import org.ansj.domain.Term;
import org.ansj.splitWord.analysis.ToAnalysis;

public class WordSegmentTest {
  public static void main(String[] args) {
    String str = "人生若能永远像两三岁小孩，本来没有责任";
    List<Term> term = ToAnalysis.parse(str);
    for (int i = 0; i < term.size(); i++) {
      String words = term.get(i).getName();// 获取单词
      String nominal = term.get(i).getNatureStr();// 获取词性
      System.out.print(words + "\t" + nominal + "\n");
    }
  }
}  