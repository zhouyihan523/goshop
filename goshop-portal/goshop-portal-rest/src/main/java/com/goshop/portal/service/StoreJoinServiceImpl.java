package com.goshop.portal.service;

import com.goshop.common.exception.MapperException;
import com.goshop.common.utils.JsonUtils;
import com.goshop.manager.i.GoodsClassService;
import com.goshop.manager.i.StoreClassService;
import com.goshop.manager.i.StoreGradeService;
import com.goshop.manager.mapper.StoreJoinMapper;
import com.goshop.manager.mapper.StoreMapper;
import com.goshop.manager.model.JsonManagement;
import com.goshop.manager.model.JsonManagementClass;
import com.goshop.manager.pojo.*;
import com.goshop.portal.i.StoreInfoModel;
import com.goshop.portal.i.StoreJoinService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.List;

@Service("storeJoinService")
public class StoreJoinServiceImpl implements StoreJoinService {

    protected final Log logger = LogFactory.getLog(this.getClass());

    @Autowired
    StoreJoinMapper storeJoinMapper;

    @Autowired
    StoreClassService storeClassService;

    @Autowired
    StoreGradeService storeGradeService;

    @Autowired
    GoodsClassService goodsClassService;

    @Autowired
    StoreMapper storeMapper;

    @Override
    public void applySeller(User user,StoreJoin storeJoin) {
        StoreJoin userStoreJoin = this.getCurrentUserStoreJoin(user.getId());
        storeJoin.setMemberId(user.getId());
        if (userStoreJoin == null) {
            storeJoin.setMemberName(user.getLoginName());
            storeJoinMapper.insert(storeJoin);
        } else {
            storeJoinMapper.updateByPrimaryKeySelective(storeJoin);
        }
    }

    @Override
    public StoreInfoModel applySellerThree(User user, StoreJoin storeJoin) {
        StoreJoin userStoreJoin = getCurrentUserStoreJoin(user.getId());
        Assert.notNull(userStoreJoin, "请先执行第一步！");
        storeJoin.setMemberId(user.getId());
        storeJoinMapper.updateByPrimaryKeySelective(storeJoin);
        //店铺分类
        List<StoreClass> storeClassParentList = storeClassService.findTreeByParentId(null);
        //店铺等级
        List<StoreGrade> storeGradeList = storeGradeService.findAll();
        //经营类目
        List<GoodsClass> goodsClassParentList = goodsClassService.findByGcParentId(0);

        StoreInfoModel storeInfoModel = new StoreInfoModel();
        storeInfoModel.setStoreClassParentList(storeClassParentList);
        storeInfoModel.setStoreGradeList(storeGradeList);
        storeInfoModel.setGoodsClassParentList(goodsClassParentList);
        return storeInfoModel;
    }

    @Override
    public boolean verificationSellerName(String sellerName,Long userId) {
        List<StoreJoin> list=storeJoinMapper.findBySellerName(sellerName);
        if(list.size()>1){
            throw new MapperException("数据异常");
        }else if(list.size()==1){
            if(list.get(0).getMemberId()!=userId){
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean verificationStoreName(String storeName,Long userId) {
        List<StoreJoin> list=storeJoinMapper.findByStoreName(storeName);
        if(list.size()>1){
            throw new MapperException("数据异常");
        }else if(list.size()==1){
            if(list.get(0).getMemberId()!=userId){
                return false;
            }
        }
        return true;
    }

    @Override
    public StoreJoin save(User user,StoreJoin storeJoin, String[] ids, String[] names) {
        StoreJoin userStoreJoin = getCurrentUserStoreJoin(user.getId());
        Assert.notNull(userStoreJoin, "请先执行第一步!");
        Assert.isTrue(ids.length == names.length, "经营类目,数据异常!");
        storeJoin.setMemberId(user.getId());

        List<JsonManagement> jsonManagementList = new ArrayList<>();


        for (int i = 0; i < ids.length; i++) {
            JsonManagement jm = new JsonManagement();

            String[] idArray = ids[i].split(",");
            String[] nameArray = names[i].split(",");
            if (ids.length == 4 && idArray.length == 1) {
                List<JsonManagementClass> JsonManagementClassList = new ArrayList<>();
                for (int y = 0; y < 3; y++) {
                    try {
                        JsonManagementClass jsonManagementClass = new JsonManagementClass();
                        jsonManagementClass.setId(ids[y]);
                        jsonManagementClass.setName(names[y]);
                        JsonManagementClassList.add(jsonManagementClass);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                jm.setJmcs(JsonManagementClassList);
                jsonManagementList.add(jm);
                break;
            } else {
                List<JsonManagementClass> JsonManagementClassList = new ArrayList<>();
                for (int y = 0; y < idArray.length; y++) {
                    try {
                        JsonManagementClass jsonManagementClass = new JsonManagementClass();
                        jsonManagementClass.setId(idArray[y]);
                        jsonManagementClass.setName(nameArray[y]);
                        JsonManagementClassList.add(jsonManagementClass);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                jm.setJmcs(JsonManagementClassList);
                jsonManagementList.add(jm);
            }
        }
        storeJoin.setStoreClassIds(JsonUtils.objectToJson(jsonManagementList));
        storeJoin.setJoininState(StoreJoinMapper.JOIN_STATIC_APPLY);
        storeJoinMapper.updateByPrimaryKeySelective(storeJoin);
        return storeJoinMapper.selectByPrimaryKey(user.getId());
    }

    @Override
    public Store getCurrentStore(User user) {
        return storeMapper.findByMemberId(user.getId());
    }

    @Override
    public StoreJoin getCurrentUserStoreJoin(User user) {
        return storeJoinMapper.selectByPrimaryKey(user.getId());
    }

    @Override
    public int paySave(StoreJoin storeJoin) {
        storeJoin.setJoininState(storeJoinMapper.JOIN_STATIC_PAY);
        return storeJoinMapper.updateByPrimaryKey(storeJoin);
    }

    private StoreJoin getCurrentUserStoreJoin(Long userId) {
        return storeJoinMapper.selectByPrimaryKey(userId);
    }
}
