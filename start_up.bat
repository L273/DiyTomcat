del /q bootstarp.jar

REM 一个主入口
REM 一个comm加载器，即要使用的全部资源
jar cvf0 bootstarp.jar -C ./out/production/DiyTomcat com/ddd/server/Bootstrap.class -C ./out/production/DiyTomcat com/ddd/classloader/CommonClassLoader.class

del /q lib/diytomcat.jar

cd out
cd production
cd Diytomcat

REM 用于包装全部的类，用于之后的加载器的调用
REM 因为在comm加载器中，已经扫描了lib中的全部jar包
jar cvf0 ../../../lib/diytomcat.jar *

cd ..
cd ..
cd ..

REM 从主入口启动程序即可
java -cp bootstarp.jar com.ddd.server.Bootstrap
pause