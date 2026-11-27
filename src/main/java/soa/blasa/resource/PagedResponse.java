package soa.blasa.resource;

import java.util.List;

import javax.ws.rs.core.Response;

import soa.blasa.util.Paging;

/**
 * Pagination OPTIONNELLE des listes (?page=1&size=20).
 *  - sans page ni size : la liste complete, exactement comme avant (rien ne change pour la page web et Postman) ;
 *  - avec page et/ou size : la tranche demandee (toujours un tableau JSON) + en-tetes
 *    X-Total-Count, X-Total-Pages, X-Page, X-Page-Size ;
 *  - valeurs invalides : 400.
 */
final class PagedResponse {

    private PagedResponse() { }

    static <T> Response of(List<T> all, Integer page, Integer size) {
        if (!Paging.requested(page, size)) {
            return Response.ok(all).build();
        }
        Paging.Slice<T> s;
        try {
            s = Paging.slice(all, page, size);
        } catch (IllegalArgumentException e) {
            return UserResource.error(Response.Status.BAD_REQUEST, e.getMessage());
        }
        return Response.ok(s.items())
                .header("X-Total-Count", s.total())
                .header("X-Total-Pages", s.totalPages())
                .header("X-Page", s.page())
                .header("X-Page-Size", s.size())
                .build();
    }
}
