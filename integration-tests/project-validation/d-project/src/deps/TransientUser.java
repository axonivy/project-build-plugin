package deps;

import com.acme.program.MyProgram;

public class TransientUser {

  private static void test() {
    MyProgram.class.getName(); // use from java: work out of the-box
  }

}
