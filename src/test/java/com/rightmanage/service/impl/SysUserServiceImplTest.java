package com.rightmanage.service.impl;

import com.rightmanage.entity.SysUserOrgAuth;
import com.rightmanage.mapper.SysUserOrgAuthMapper;
import com.rightmanage.service.SysModuleService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SysUserServiceImplTest {

    @InjectMocks
    private SysUserServiceImpl sysUserService;

    @Mock
    private SysUserOrgAuthMapper sysUserOrgAuthMapper;

    @Mock
    private SysModuleService sysModuleService;

    @Test
    void bindAuthorizedOrg_updatesExistingSingleTenantAuthInsteadOfInsert() {
        SysUserOrgAuth existing = new SysUserOrgAuth();
        existing.setUserId(1L);
        existing.setModuleCode("bi_workstation");
        existing.setTenantCode("");
        existing.setOrgId("OLD_ORG");

        when(sysModuleService.isMultiTenant("bi_workstation")).thenReturn(false);
        when(sysUserOrgAuthMapper.selectOne(any())).thenReturn(existing);
        when(sysUserOrgAuthMapper.update(any(SysUserOrgAuth.class), any())).thenReturn(1);

        boolean updated = sysUserService.bindAuthorizedOrg(1L, "bi_workstation", null, "NEW_ORG");

        assertTrue(updated);
        assertEquals("NEW_ORG", existing.getOrgId());
        verify(sysUserOrgAuthMapper).update(eq(existing), any());
        verify(sysUserOrgAuthMapper, never()).insert(any());
    }

    @Test
    void bindAuthorizedOrg_insertsWhenNoExistingAuthFound() {
        when(sysModuleService.isMultiTenant("bi_workstation")).thenReturn(false);
        when(sysUserOrgAuthMapper.selectOne(any())).thenReturn(null);
        when(sysUserOrgAuthMapper.insert(any(SysUserOrgAuth.class))).thenReturn(1);

        boolean inserted = sysUserService.bindAuthorizedOrg(1L, "bi_workstation", null, "NEW_ORG");

        assertTrue(inserted);

        ArgumentCaptor<SysUserOrgAuth> captor = ArgumentCaptor.forClass(SysUserOrgAuth.class);
        verify(sysUserOrgAuthMapper).insert(captor.capture());

        SysUserOrgAuth saved = captor.getValue();
        assertEquals(Long.valueOf(1L), saved.getUserId());
        assertEquals("bi_workstation", saved.getModuleCode());
        assertEquals("", saved.getTenantCode());
        assertEquals("NEW_ORG", saved.getOrgId());
    }
}
