package xyz.zhiwei.cognitivedesign.dao.accessimpl.read;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import xyz.zhiwei.cognitivedesign.dao.accessimpl.DaoBeanCache;
import xyz.zhiwei.cognitivedesign.morphism.principle.source.container.PrincipleSourceLane;
import xyz.zhiwei.cognitivedesign.morphism.principle.source.qualifier.QualifiersLane;


/**
 * 本原集取实现
 */
public class ReadAccess {
    private static final Logger log = LoggerFactory.getLogger(ReadAccess.class);

    
    private Executor daoScheduleExecutor;
    private ReadLane readLane;
    
    
    
    public ReadAccess(DaoBeanCache daoBeanCache,Executor daoScheduleExecutor,Executor daoReadExecutor) {
    	this.daoScheduleExecutor=daoScheduleExecutor;
    	this.readLane=new ReadLane(daoBeanCache, daoReadExecutor);
    }

    
    
    /**
     * 读取指定数据集
     * 根据查询泳道列表，返回各泳道的查询结果集。
     * @param qualifiersLaneList
     * @return
     */
    public List<PrincipleSourceLane> query(List<QualifiersLane> qualifiersLaneList){
		if(null ==qualifiersLaneList || qualifiersLaneList.isEmpty()) {
	        log.info("查询泳道列表为空");
			return new ArrayList<>();
		}

        List<CompletableFuture<PrincipleSourceLane>> futures = IntStream
                .range(0, qualifiersLaneList.size())
                .mapToObj(laneIndex -> CompletableFuture.supplyAsync(
                        () -> this.readLane.queryLane(qualifiersLaneList.get(laneIndex), laneIndex),
                        this.daoScheduleExecutor))
                .collect(Collectors.toList());
    	
    	List<PrincipleSourceLane> principleSourceLaneList= IntStream.range(0, futures.size())
    			.mapToObj(i -> {
                    try {
                        return futures.get(i).get();
                    } catch (Exception e) {
                        log.error("第{}号泳道查询执行异常", i, e);
                        return new PrincipleSourceLane();
                    }
                })
    			.collect(Collectors.toList());
    	
    	return principleSourceLaneList;
    }
    

    
}
