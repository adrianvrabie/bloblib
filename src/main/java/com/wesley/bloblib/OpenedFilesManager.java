package com.wesley.bloblib;

import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import org.pmw.tinylog.Logger;

import com.microsoft.azure.storage.AccessCondition;
import com.microsoft.azure.storage.blob.CloudBlob;
import com.wesley.bloblib.BfsCacheBase;

public class OpenedFilesManager  extends BfsCacheBase {
	private static OpenedFilesManager instance;
	private static ExecutorService leaseAutoRenewES;
//	private ThreadPoolExecutor leaseAutoRenwerTP = new ThreadPoolExecutor(1, 1, 2, TimeUnit.SECONDS,null);
	
	private OpenedFilesManager(){
		cacheStore = new ConcurrentHashMap<String, CachedObject>(Constants.OPENED_FILE_MANAGER_INIT_CAPACITY, 0.9f, 1);
		capacity = Constants.OPENED_FILE_MANAGER_MAX_CAPACITY;
		expireTime = Constants.OPENED_FILE_MANAGER_EXPIRE_TIME;
//		/* start the lease auto renew service */
		startLeaseAutoRenewService();
	}
	
	@Override
	public boolean put(Long cacheKey, Object cached) throws BfsException {
		// TODO Auto-generated method stub
		return super.put(cacheKey, cached);
	}
	
	@Override
	public void delete(Long cacheKey) {
		// TODO Auto-generated method stub
		super.delete(cacheKey);
	}
		
	@Override
	public Object get(Long cacheKey) {
		// TODO Auto-generated method stub
		return super.get(cacheKey);
	}
	
	@Override
	public int count() {
		// TODO Auto-generated method stub
		return super.count();
	}
	
	private final void startLeaseAutoRenewService(){
		/* Make it a daemon */
		leaseAutoRenewES = Executors.newSingleThreadExecutor(new ThreadFactory(){
		    public Thread newThread(Runnable r) {
		        Thread t = new Thread(r);
		        t.setDaemon(true);
		        return t;
		    }        
		});
		leaseAutoRenewES.submit(new leaseAutoRenwer());
	}
	
	@Override
	public void finalize() {
		leaseAutoRenewES.shutdown();
	}
	
	public static OpenedFilesManager getInstance(){
        if(instance == null){
            synchronized (OpenedFilesManager.class) {
                if(instance == null){
                    instance = new OpenedFilesManager();
                }
            }
        }
        return instance;
    }
	
	private class leaseAutoRenwer implements Runnable{
		@SuppressWarnings("static-access")
		@Override
		public void run() {
			try {				
				/* start the lease auto renew process */
				while (true){
					OpenedFilesManager openedFilesManager = OpenedFilesManager.getInstance();
					for (Entry<String, CachedObject> entry : openedFilesManager.cacheStore.entrySet()) {
					    OpenedFileModel openedFile = (OpenedFileModel)openedFilesManager.get(entry.getKey());
						if (null != openedFile.getLeaseID()){
							CloudBlob blob;
							AccessCondition accCondtion = new AccessCondition();
							BlobReqParams arParams = new BlobReqParams();
							arParams.setContainer(openedFile.getContainer());
							arParams.setBlob(openedFile.getBlob());
							accCondtion.setLeaseID(openedFile.getLeaseID());
							blob = BlobService.getBlobReference(arParams);
							blob.renewLease(accCondtion);
							
						}
					}
					Thread.sleep(Constants.DEFAULT_OFM_THREAD_SLEEP_MILLS);
				}			
			} catch (Exception ex) {
				Logger.error(ex.getMessage());
			}
		}
	
	}
}
