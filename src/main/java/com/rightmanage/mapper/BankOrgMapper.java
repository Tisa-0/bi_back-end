package com.rightmanage.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.rightmanage.entity.BankOrg;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface BankOrgMapper extends BaseMapper<BankOrg> {
    List<BankOrg> selectActiveAll(@Param("today") String today);

    List<BankOrg> selectSubOrgList(@Param("orgcod") String orgcod, @Param("today") String today);

    Map<String, Object> selectOrgFlattenRow(@Param("tableName") String tableName,
                                            @Param("dte") String dte,
                                            @Param("orgcod") String orgcod);

    List<String> selectNextLevelOrgCodes(@Param("tableName") String tableName,
                                         @Param("dte") String dte,
                                         @Param("orgcod") String orgcod);

    List<String> selectAllSubOrgCodes(@Param("tableName") String tableName,
                                      @Param("dte") String dte,
                                      @Param("orgcod") String orgcod);
}
