package com.lyy.video.entity.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 治理动作入参
 * 四个接口共用：通过/重新上架忽略 reason，驳回必填，下架可选。
 * 由于「是否必填」随动作变化，校验放在 Service 而非注解。
 */
@Data
public class ReviewActionDTO implements Serializable {

    /** 驳回原因 / 下架原因 */
    private String reason;
}
