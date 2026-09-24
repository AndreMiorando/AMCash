package br.com.amcash.entry.dto.response;

import java.util.List;

public record CreatedEntriesResponse(
        List<EntryResponse> entries
) {
}
