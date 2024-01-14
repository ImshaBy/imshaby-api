package by.imsha.meilisearch.writer.feign;

import by.imsha.meilisearch.writer.feign.dto.request.DeleteDocumentsByFilterRequest;
import com.meilisearch.sdk.model.TaskInfo;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

public interface MeilisearchApiFeignClient {

    @PostMapping("/indexes/{index_uid}/documents/delete")
    TaskInfo deleteDocuments(@PathVariable("index_uid") String indexUid,
                             @RequestBody DeleteDocumentsByFilterRequest deleteDocumentsByFilterRequest);
}
