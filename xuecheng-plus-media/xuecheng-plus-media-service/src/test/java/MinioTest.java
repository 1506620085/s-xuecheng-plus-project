import com.xuecheng.base.exception.XueChengPlusException;
import io.minio.*;
import io.minio.messages.DeleteError;
import io.minio.messages.DeleteObject;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 测试 Minio 上传方法
 */
public class MinioTest {

    //将分块文件上传至minio
    @Test
    public void uploadChunk() throws IOException {
        //分块文件存储路径
        String chunkFilePath = "G:\\a_Download_Area\\a_BaiduNetdiskDownload\\预科作业3.0\\chunk_test\\";
        File chunkFile = new File(chunkFilePath);
        File[] chunkFiles = chunkFile.listFiles();
        List<File> files = Arrays.stream(chunkFiles).sorted(new Comparator<File>() {
            @Override
            public int compare(File o1, File o2) {
                return Integer.parseInt(o1.getName()) - Integer.parseInt(o2.getName());
            }
        }).collect(Collectors.toList());
        try {
            MinioClient minioClient =
                    MinioClient.builder()
                            .endpoint("http://127.0.0.1:9000")
                            .credentials("ouzehangminio", "ouzehangminio")
                            .build();
            boolean found =
                    minioClient.bucketExists(BucketExistsArgs.builder().bucket("xuecheng-video").build());
            if (!found) {
                // Make a new bucket called 'asiatrip'.
                minioClient.makeBucket(MakeBucketArgs.builder().bucket("xuecheng-video").build());
            } else {
                System.out.println("Bucket 'xuecheng-video' already exists.");
            }
            for (File file : files) {
                minioClient.uploadObject(
                        UploadObjectArgs.builder()
                                .bucket("xuecheng-video")
                                .object(file.getName())
                                .filename(chunkFilePath + file.getName())
                                .build());
            }
        } catch (Exception e) {
            XueChengPlusException.cast(e.toString());
        }
    }

    //合并文件，Minio要求分块文件最小5M
    @Test
    public void test_merge() throws Exception {
        MinioClient minioClient =
                MinioClient.builder()
                        .endpoint("http://127.0.0.1:9000")
                        .credentials("ouzehangminio", "ouzehangminio")
                        .build();
        List<ComposeSource> sourceObjectList = Stream.iterate(0, i -> ++i).limit(10).map(
                i -> ComposeSource.builder().bucket("xuecheng-video").object(String.valueOf(i)).build()
        ).collect(Collectors.toList());

        minioClient.composeObject(
                ComposeObjectArgs.builder()
                        .bucket("xuecheng-video")
                        .object("mergeTest.pdf")
                        .sources(sourceObjectList)
                        .build());
    }

    //清除分块文件
    @Test
    public void test_removeObjects() throws Exception{
        MinioClient minioClient =
                MinioClient.builder()
                        .endpoint("http://127.0.0.1:9000")
                        .credentials("ouzehangminio", "ouzehangminio")
                        .build();
        List<DeleteObject> objects = Stream.iterate(0, i -> ++i).limit(10).map(i -> new DeleteObject(String.valueOf(i))).collect(Collectors.toList());
        Iterable<Result<DeleteError>> results =
                minioClient.removeObjects(
                        RemoveObjectsArgs.builder().bucket("xuecheng-video").objects(objects).build());
        for (Result<DeleteError> result : results) {
            DeleteError error = result.get();
            System.out.println(
                    "Error in deleting object " + error.objectName() + "; " + error.message());
        }
    }
}
