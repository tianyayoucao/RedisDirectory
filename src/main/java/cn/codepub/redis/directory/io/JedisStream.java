package cn.codepub.redis.directory.io;

import com.google.common.primitives.Longs;
import lombok.extern.log4j.Log4j2;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Transaction;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import static cn.codepub.redis.directory.utils.FileBlocksUtil.getBlockName;
import static cn.codepub.redis.directory.utils.FileBlocksUtil.getBlockSize;

/**
 * <p>
 * Created by wangxu on 2016/12/26 16:18.
 * </p>
 * <p>
 * Description: TODO
 * </p>
 *
 * @author Wang Xu
 * @version V1.0.0
 * @since V1.0.0 <br></br>
 * WebSite: http://codepub.cn <br></br>
 * Licence: Apache v2 License
 */
@Log4j2
public class JedisStream implements InputOutputStream {
    private String IP;
    private int port;
    private int timeout = 3000;

    private Jedis openJedis() {
        return new Jedis(IP, port, timeout);
    }

    public JedisStream(String IP, int port, int timeout) {
        this(IP, port);
        this.timeout = timeout;
    }

    public JedisStream(String IP, int port) {
        this.IP = IP;
        this.port = port;
    }

    @Override
    public Boolean hexists(byte[] key, byte[] field) {
        Jedis jedis = openJedis();
        Boolean hexists = jedis.hexists(key, field);
        jedis.close();
        return hexists;
    }

    @Override
    public byte[] hget(byte[] key, byte[] field) {
        Jedis jedis = openJedis();
        byte[] hget = jedis.hget(key, field);
        jedis.close();
        return hget;
    }

    @Override
    public void close() throws IOException {
        //Noop
    }

    @Override
    public Long hdel(byte[] key, byte[]... fields) {
        Jedis jedis = openJedis();
        Long hdel = jedis.hdel(key, fields);
        jedis.close();
        return hdel;
    }

    @Override
    public Long hset(byte[] key, byte[] field, byte[] value) {
        Jedis jedis = openJedis();
        Long hset = jedis.hset(key, field, value);
        jedis.close();
        return hset;
    }

    @Override
    public Set<byte[]> hkeys(byte[] key) {
        Jedis jedis = openJedis();
        Set<byte[]> hkeys = jedis.hkeys(key);
        jedis.close();
        return hkeys;
    }

    @Override
    public void deleteFile(String fileLengthKey, String fileDataKey, String field, long blockSize) {
        Jedis jedis = openJedis();
        Transaction multi = jedis.multi();
        //delete file length
        multi.hdel(fileLengthKey.getBytes(), field.getBytes());
        //delete file content
        for (int i = 0; i < blockSize; i++) {
            byte[] blockName = getBlockName(field, i);
            multi.hdel(fileDataKey.getBytes(), blockName);
        }
        List<Object> exec = multi.exec();
        checkTransactionResult(exec);
        multi.clear();
        jedis.close();
    }

    @Override
    public void rename(String fileLengthKey, String fileDataKey, String oldField, String newField, List<byte[]> values, long
            fileLength) {
        Jedis jedis = openJedis();
        Transaction multi = jedis.multi();
        //add new file length
        multi.hset(fileLengthKey.getBytes(), newField.getBytes(), Longs.toByteArray(fileLength));
        //add new file content
        Long blockSize = getBlockSize(fileLength);
        for (int i = 0; i < blockSize; i++) {
            multi.hset(fileDataKey.getBytes(), getBlockName(newField, i), values.get(i));
        }
        List<Object> exec = multi.exec();
        checkTransactionResult(exec);
        multi.clear();
        jedis.close();
        deleteFile(fileLengthKey, fileDataKey, oldField, blockSize);
    }
}
