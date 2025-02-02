/*
 * Command Patternでもないかこれ？
 * コマンドクラスをまとめて一つのコマンドクラスにする
 * 
 */
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

 class MergedCommand implements ICommand {
      
      private List<ICommand> commands;
      
      public MergedCommand() {
            commands = new ArrayList<ICommand>();
      }
      
      public void addCommand(ICommand command) {
            commands.add(command);
      }
      
      @Override
      public void execute(InputStream in, OutputStream out) throws IOException {
        //x番目の出力をx+1番目の入力にする
        commands.get(0).
        
        //threadを使って並列処理
        for (ICommand command : commands) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        command.execute(in, out);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }).start();
      }
      
     }