package com.heypixel.heypixelmod.obsoverlay.commands.impl;

import com.heypixel.heypixelmod.obsoverlay.commands.Command;
import com.heypixel.heypixelmod.obsoverlay.commands.CommandInfo;
import com.heypixel.heypixelmod.obsoverlay.utils.ChatUtils;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

@CommandInfo(
   name = "irc",
   description = "Send message to IRC server",
   aliases = {}
)
public class CommandIRC extends Command {
   private static final String SERVER_HOST = "127.0.0.1";
   private static final int SERVER_PORT = 34797;
   private static final ExecutorService executor = Executors.newFixedThreadPool(2);
   private static Socket socket = null;
   private static PrintWriter out = null;
   private static BufferedReader in = null;
   private static final BlockingQueue<String> messageQueue = new LinkedBlockingQueue<>();
   private static boolean connected = false;
   
   static {
      // 启动连接管理线程
      executor.submit(() -> {
         while (true) {
            if (!connected) {
               connectToServer();
            }
            try {
               Thread.sleep(5000);
            } catch (InterruptedException e) {
               break;
            }
         }
      });
      

      executor.submit(() -> {
         while (true) {
            try {
               String message = messageQueue.take();
               if (connected && out != null) {
                  out.println(message);
               }
            } catch (InterruptedException e) {
               break;
            }
         }
      });
   }
   
   private static void connectToServer() {
      try {
         if (socket != null) {
            try {
               socket.close();
            } catch (IOException e) {

            }
         }
         
         socket = new Socket(SERVER_HOST, SERVER_PORT);
         out = new PrintWriter(socket.getOutputStream(), true);
         in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
         connected = true;
         

         executor.submit(() -> {
            try {
               String response;
               while (connected && (response = in.readLine()) != null) {
                  ChatUtils.addChatMessage(response);
               }
            } catch (IOException e) {

            } finally {
               connected = false;
            }
         });
         
      } catch (IOException e) {
         connected = false;
         socket = null;
         out = null;
         in = null;
      }
   }
   
   @Override
   public void onCommand(String[] args) {
      if (args.length == 0) {
         ChatUtils.addChatMessage("Usage: .irc <message>");
         return;
      }
      
      String message = String.join(" ", args);
      
      // 如果是命令，直接发送并等待服务器响应
      if (message.startsWith("/")) {
         messageQueue.offer(message.substring(1));
      } else {
         messageQueue.offer(message);
      }
      
      // 如果未连接，尝试连接
      if (!connected) {
         connectToServer();
      }
   }
   
   // 静态方法供其他模块调用
   public static void sendCommand(String command) {
      messageQueue.offer(command);
      if (!connected) {
         connectToServer();
      }
   }

   @Override
   public String[] onTab(String[] args) {
      return new String[0];
   }
}