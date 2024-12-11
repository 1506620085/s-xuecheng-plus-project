import org.apache.commons.codec.digest.DigestUtils;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 测试大文件上传方法
 */
public class BigFileTest {

    //分块测试
    @Test
    public void testChunk() throws IOException {
        // 读取文件
        File sourceFile = new File("G:\\a_Download_Area\\a_BaiduNetdiskDownload\\预科作业3.0\\《预科作业2.0》.pdf");
        //分块文件存储路径
        String chunkFilePath = "G:\\a_Download_Area\\a_BaiduNetdiskDownload\\预科作业3.0\\chunk_test\\";
        //使用流从源文件读数据，向分块文件中写数据
        RandomAccessFile r = new RandomAccessFile(sourceFile, "r");
        // 文件大小
        long fileLength = sourceFile.length();
        // 分块大小
        int chunkLength = 1024 * 1024 * 5;
        // 分块数量
        int chunkNum = (int) Math.ceil(fileLength * 1.0 / chunkLength);
        // 缓存区
        byte[] bytes = new byte[1024];
        for (int i = 0; i < chunkNum; i++) {
            File chunkFile = new File(chunkFilePath + i);
            // 分块文件写入流
            RandomAccessFile rw = new RandomAccessFile(chunkFile, "rw");
            int len = -1;
            while ((len = r.read(bytes)) != -1) {
                rw.write(bytes, 0, len);
                if (chunkFile.length() >= chunkLength) {
                    break;
                }
            }
            rw.close();
        }
        r.close();
    }

    //将分块进行合并
    @Test
    public void testMerge() throws IOException {
        // 读取分块存储路径
        File chunkFilesPath = new File("G:\\a_Download_Area\\a_BaiduNetdiskDownload\\预科作业3.0\\chunk_test");
        // 获取分块文件列表
        File[] chunkFiles = chunkFilesPath.listFiles();
        // 进行排序
        List<File> chunkFileCollect = Arrays.stream(chunkFiles).sorted(new Comparator<File>() {
            @Override
            public int compare(File o1, File o2) {
                return Integer.parseInt(o1.getName()) - Integer.parseInt(o2.getName());
            }
        }).collect(Collectors.toList());
        // 缓冲区
        byte[] bytes = new byte[1024];
        File mergeFile = new File("G:\\a_Download_Area\\a_BaiduNetdiskDownload\\预科作业3.0\\chunk_test\\mergeFile.pdf");
        RandomAccessFile rw = new RandomAccessFile(mergeFile, "rw");
        for (File file : chunkFileCollect) {
            RandomAccessFile r = new RandomAccessFile(file, "r");
            int len = -1;
            while ((len = r.read(bytes)) != -1) {
                rw.write(bytes,0,len);
            }
            r.close();
        }
        rw.close();
        // 合并完文件后对合并文件进行 md5 校验
        //源文件
        File sourceFile = new File("G:\\a_Download_Area\\a_BaiduNetdiskDownload\\预科作业3.0\\《预科作业2.0》.pdf");
        FileInputStream sourceFileInputStream = new FileInputStream(sourceFile);
        FileInputStream mergeFileInputStream = new FileInputStream(mergeFile);
        String sourceFileInputStreamMd5 = DigestUtils.md5Hex(sourceFileInputStream);
        String mergeFileInputStreamMd5 = DigestUtils.md5Hex(mergeFileInputStream);
        if (sourceFileInputStreamMd5.equals(mergeFileInputStreamMd5)) {
            System.out.println("文件合并成功");
        }
    }
}
