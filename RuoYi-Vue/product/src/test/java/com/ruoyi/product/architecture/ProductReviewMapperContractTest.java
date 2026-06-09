package com.ruoyi.product.architecture;

import static org.junit.Assert.assertTrue;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.Test;

public class ProductReviewMapperContractTest
{
    @Test
    public void reviewListAndDetailMustReturnWarehouseKindSummary() throws IOException
    {
        String source = readMapperSource();

        assertTrue("product review mapper must join warehouse kind summary",
                source.contains("<sql id=\"reviewWarehouseKindSummaryJoin\">")
                        && source.contains("from product_spu_warehouse")
                        && source.contains("warehouse_kind_summary"));
        assertTrue("product review mapper must keep existing warehouse kind codes",
                source.contains("when r.warehouse_summary in ('official', 'third_party', 'MIXED') then r.warehouse_summary"));
        assertTrue("product review mapper must fallback historical warehouse names to aggregated kind",
                source.contains("when wh.warehouse_kind_summary in ('official', 'third_party', 'MIXED') then wh.warehouse_kind_summary"));
        assertTrue("product review mapper must normalize legacy non-empty warehouse text as third party",
                source.contains("when r.warehouse_summary is not null and r.warehouse_summary != '' then 'third_party'"));
        assertTrue("selectReviewList must use normalized review columns",
                source.contains("<select id=\"selectReviewList\"")
                        && source.contains("<include refid=\"reviewRequestSelectColumns\"/>")
                        && source.contains("<include refid=\"reviewWarehouseKindSummaryJoin\"/>"));
        assertTrue("selectReviewById must use normalized review columns",
                source.contains("<select id=\"selectReviewById\"")
                        && source.indexOf("<select id=\"selectReviewById\"") < source.lastIndexOf("<include refid=\"reviewRequestSelectColumns\"/>")
                        && source.indexOf("<select id=\"selectReviewById\"") < source.lastIndexOf("<include refid=\"reviewWarehouseKindSummaryJoin\"/>"));
    }

    private String readMapperSource() throws IOException
    {
        Path path = Paths.get("src/main/resources/mapper/product/ProductReviewMapper.xml");
        return Files.readString(path, StandardCharsets.UTF_8);
    }
}
