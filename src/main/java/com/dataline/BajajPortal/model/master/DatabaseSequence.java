package com.dataline.BajajPortal.model.master;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document(collection = "sequences")
public class DatabaseSequence {
    @Id
    private String id;
    private long seq;
}