package com.ruoyi.system.mapper;

import java.util.Date;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.system.domain.PortalDirectLoginTicket;

/**
 * Portal direct-login ticket mapper.
 */
public interface PortalDirectLoginTicketMapper
{
    public int insertPortalDirectLoginTicket(PortalDirectLoginTicket ticket);

    public PortalDirectLoginTicket selectPortalDirectLoginTicketByTokenHash(String tokenHash);

    public int markPortalDirectLoginTicketUsed(@Param("ticketId") Long ticketId,
            @Param("usedTime") Date usedTime, @Param("usedIp") String usedIp,
            @Param("updateBy") String updateBy);

    public int markPortalDirectLoginTicketExpired(@Param("ticketId") Long ticketId,
            @Param("updateBy") String updateBy);
}
