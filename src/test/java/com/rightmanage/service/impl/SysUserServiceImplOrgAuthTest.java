package com.rightmanage.service.impl;

import com.rightmanage.entity.BankOrg;
import com.rightmanage.mapper.BankOrgMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SysUserServiceImplOrgAuthTest {

    @Spy
    @InjectMocks
    private SysUserServiceImpl sysUserService;

    @Mock
    private BankOrgMapper bankOrgMapper;

    @Test
    void isUserAuthorizedForOrgLevel_fallsBackToParentChainWhenFlattenTableHasNoRow() {
        BankOrg userOrg = new BankOrg();
        userOrg.setId("ORG_PARENT");

        BankOrg sourceOrg = new BankOrg();
        sourceOrg.setId("ORG_CHILD");
        sourceOrg.setParentId("ORG_PARENT");

        BankOrg parentOrg = new BankOrg();
        parentOrg.setId("ORG_PARENT");
        parentOrg.setParentId(null);

        doReturn(userOrg).when(sysUserService).getAuthorizedOrg(1L, "moduleA", null);
        when(bankOrgMapper.selectOrgFlattenRow(anyString(), anyString(), eq("ORG_CHILD"))).thenReturn(null);
        when(bankOrgMapper.selectActiveAll(anyString())).thenReturn(Arrays.asList(sourceOrg, parentOrg));

        assertTrue(sysUserService.isUserAuthorizedForOrgLevel(1L, "moduleA", null, "ORG_CHILD"));
    }
}
