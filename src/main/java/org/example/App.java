package org.example;

import org.example.config.Config;
import org.example.dao.BookDao;
import org.example.model.Book;
import org.example.util.Uppercaser;

import java.util.*;
import java.util.concurrent.*;

public class App {
    public static void main(String[] args) throws Exception {
        System.out.println("DB_URL=" + Config.DB_URL);

        BookDao dao = new BookDao();
        dao.ensureSchemaAndData();

        // Пул №1: I/O (reading and updating)
        ExecutorService ioPool = Executors.newCachedThreadPool();
        // Пул №2 modification in memory
        ForkJoinPool modifyPool = new ForkJoinPool();


        List<Book> shared = Collections.synchronizedList(new ArrayList<>(3000));

        // Фаза 1: чтение из БД
        long[] mmc = dao.minMaxAndCount();
        long minId = mmc[0], maxId = mmc[1], total = mmc[2];
        if (total == 0) {
            System.out.println("В таблице books нет данных.");
            ioPool.shutdown(); modifyPool.shutdown();
            return;
        }
        System.out.printf("ID-range: [%d..%d], rows=%d%n", minId, maxId, total);

        List<long[]> ranges = makeRanges(minId, maxId, Config.READ_CHUNK_SIZE);
        CountDownLatch readLatch = new CountDownLatch(ranges.size());
        for (long[] r : ranges) {
            ioPool.submit(() -> {
                try {
                    //logs
                    System.out.println("read by " + Thread.currentThread().getName()
                            + " range=[" + r[0] + ".." + r[1] + "]");

                    List<Book> chunk = dao.readRange(r[0], r[1]);
                    if (!chunk.isEmpty()) {
                        synchronized (shared) {
                            shared.addAll(chunk);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    readLatch.countDown();
                }
            });
        }
        readLatch.await();
        System.out.println("Фаза 1: загружено в список: " + shared.size());

        // Фаза 2: модификация (UPPERCASE всех String-полей)
        List<int[]> parts = indexPartitions(shared.size(), Config.MODIFY_PART_SIZE);
        CountDownLatch modifyLatch = new CountDownLatch(parts.size());
        for (int[] p : parts) {
            final int from = p[0];
            final int to   = p[1];

            modifyPool.execute(() -> {
                try {

                    System.out.println("modify by " + Thread.currentThread().getName()
                            + " slice=[" + from + ".." + (to - 1) + "]");

                    for (int i = from; i < to; i++) {


                        Book b;
                        synchronized (shared) { b = shared.get(i); }
                        Uppercaser.toUpperCaseStrings(b);
                    }
                } finally {
                    modifyLatch.countDown();
                }
            });
        }

        modifyLatch.await();
        System.out.println("Фаза 2: строки переведены в UPPERCASE.");

        //  Фаза 3: обновление БД батчами
        List<List<Book>> batches = batch(shared, Config.UPDATE_BATCH_SIZE);
        CountDownLatch updateLatch = new CountDownLatch(batches.size());
        for (List<Book> batch : batches) {
            ioPool.submit(() -> {
                try {
                    System.out.println("update by " + Thread.currentThread().getName()
                            + " batchSize=" + batch.size());

                    dao.updateBatch(batch);
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    updateLatch.countDown();
                }
            });
        }
        updateLatch.await();
        System.out.println("Фаза 3: обновлено строк: " + shared.size());

        ioPool.shutdown();
        modifyPool.shutdown();
        ioPool.awaitTermination(30, TimeUnit.SECONDS);
        modifyPool.awaitTermination(30, TimeUnit.SECONDS);
    }

    private static List<long[]> makeRanges(long minId, long maxId, int step) {
        List<long[]> res = new ArrayList<>();
        for (long s = minId; s <= maxId; s += step) {
            long e = Math.min(s + step - 1, maxId);
            res.add(new long[]{s, e});
        }
        return res;
    }

    private static List<int[]> indexPartitions(int n, int size) {
        List<int[]> res = new ArrayList<>();
        for (int i = 0; i < n; i += size) {
            res.add(new int[]{i, Math.min(i + size, n)});
        }
        return res;
    }

    private static List<List<Book>> batch(List<Book> list, int bs) {
        List<List<Book>> res = new ArrayList<>();
        for (int i = 0; i < list.size(); i += bs) {
            List<Book> sub;
            synchronized (list) { sub = new ArrayList<>(list.subList(i, Math.min(i + bs, list.size()))); }
            res.add(sub);
        }
        return res;
    }
}
