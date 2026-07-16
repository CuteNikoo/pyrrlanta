package com.pyrrlanta.pyrrlantatribes.tribe;

import com.flowpowered.math.vector.Vector2d;
import com.flowpowered.math.vector.Vector3d;

import de.bluecolored.bluemap.api.BlueMapAPI;
import de.bluecolored.bluemap.api.BlueMapMap;
import de.bluecolored.bluemap.api.BlueMapWorld;
import de.bluecolored.bluemap.api.markers.HtmlMarker;
import de.bluecolored.bluemap.api.markers.MarkerSet;
import de.bluecolored.bluemap.api.markers.ShapeMarker;
import de.bluecolored.bluemap.api.math.Color;
import de.bluecolored.bluemap.api.math.Shape;

import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;

import com.pyrrlanta.pyrrlantatribes.PyrrlantaTribes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

// Soft integration with BlueMap (https://bluemap.bluecolored.de/): draws each tribe's
// claimed territory as a single merged outline (with holes for unclaimed pockets) on the web
// map, Towny-style, plus an always-visible name label at its center.
//
// This class references BlueMap's API classes directly, so it must only ever be touched
// (even just classloaded) when the "bluemap" mod is confirmed present -- see the
// ModList.isLoaded("bluemap") check in PyrrlantaTribes. Never reference
// this class unconditionally, or servers without BlueMap installed will crash with a
// NoClassDefFoundError.
//
// Dynmap's Fabric build has no public API for third-party mods to hook into (its MarkerAPI
// sits behind a package-private field with no readiness event, unlike BlueMap's clean
// BlueMapAPI.onEnable() callback), so BlueMap is used here too, same as the NeoForge side.
// BlueMap markers aren't persistent -- they must be redrawn whenever BlueMap (re)loads -- so
// instead of hooking every tribe-mutating command, this just redraws everything on a fixed
// timer. Claim counts are small, so the cost of a full rebuild every few seconds is negligible.
public final class TribeMapIntegration {
    private static final String MARKER_SET_ID = "pyrrlanta-tribes";
    private static final int REFRESH_INTERVAL_TICKS = 100; // ~5 seconds
    private static final float MARKER_HEIGHT = 64; // fixed flat height; not terrain-following

    private static int tickCounter = 0;

    private TribeMapIntegration() {
    }

    public static void init() {
        BlueMapAPI.onEnable(api -> PyrrlantaTribes.LOGGER.info("BlueMap detected; tribe claim markers enabled"));
        NeoForge.EVENT_BUS.addListener(TribeMapIntegration::onServerTick);
    }

    private static void onServerTick(ServerTickEvent.Post event) {
        tickCounter++;
        if (tickCounter < REFRESH_INTERVAL_TICKS) {
            return;
        }
        tickCounter = 0;
        // refreshAll runs inside the server tick, so anything it throws propagates into
        // tickServer and kills the server. These markers are purely cosmetic and are rebuilt
        // from scratch every refresh, so a failed pass is worth a log line and nothing more --
        // never worth taking the world down for.
        BlueMapAPI.getInstance().ifPresent(api -> {
            try {
                refreshAll(api, event.getServer());
            } catch (Exception e) {
                PyrrlantaTribes.LOGGER.error("Tribe map marker refresh failed; skipping this pass", e);
            }
        });
    }

    private static void refreshAll(BlueMapAPI api, MinecraftServer server) {
        TribeSavedData data = TribeSavedData.get(server);
        for (ServerLevel level : server.getAllLevels()) {
            Optional<BlueMapWorld> world = api.getWorld(level);
            if (world.isEmpty()) {
                continue;
            }
            for (BlueMapMap map : world.get().getMaps()) {
                MarkerSet markerSet = map.getMarkerSets().computeIfAbsent(MARKER_SET_ID,
                        id -> MarkerSet.builder().label("Tribes").build());
                markerSet.getMarkers().clear();
                for (Tribe tribe : data.getAllTribes()) {
                    addTribeMarkers(markerSet, tribe, level);
                }
            }
        }
    }

    private static void addTribeMarkers(MarkerSet markerSet, Tribe tribe, ServerLevel level) {
        Set<ChunkPos> claims = new HashSet<>();
        for (ClaimPos claim : tribe.getClaims()) {
            if (claim.dimension().equals(level.dimension())) {
                claims.add(claim.chunk());
            }
        }
        if (claims.isEmpty()) {
            return;
        }

        Color fill = shade(tribe, 0.35f);
        Color line = shade(tribe, 0.9f);
        String safeName = escapeHtml(tribe.getName());
        // Admin land is server-owned and memberless, so a member count would just read "0".
        String detail = tribe.isAdminTribe()
                ? "<b>" + safeName + "</b><br>Server-protected land"
                : "<b>" + safeName + "</b><br>Members: " + tribe.getMembers().size()
                        + (tribe.isProtectionEnabled() ? "<br>Protected territory" : "<br>Open territory");

        int regionIndex = 0;
        for (Set<ChunkPos> region : groupIntoConnectedRegions(claims)) {
            regionIndex++;
            List<List<Vector2d>> loops = traceBoundaryLoops(region);
            if (loops.isEmpty()) {
                continue;
            }

            // The outer boundary is whichever traced loop encloses the most area; every other
            // loop in this region is a fully-surrounded unclaimed pocket (a hole).
            List<Vector2d> outer = loops.get(0);
            double outerArea = loopArea(outer);
            List<List<Vector2d>> holes = new ArrayList<>();
            for (List<Vector2d> loop : loops) {
                double area = loopArea(loop);
                if (area > outerArea) {
                    holes.add(outer);
                    outer = loop;
                    outerArea = area;
                } else if (loop != outer) {
                    holes.add(loop);
                }
            }

            Shape outerShape = toShape(outer);
            if (outerShape == null) {
                continue;
            }
            ShapeMarker.Builder markerBuilder = ShapeMarker.builder()
                    .label(tribe.getName())
                    .shape(outerShape, MARKER_HEIGHT)
                    .fillColor(fill)
                    .lineColor(line)
                    .lineWidth(2)
                    .detail(detail)
                    // ShapeMarker defaults depthTest to true, which occludes the flat marker
                    // wherever real terrain height exceeds MARKER_HEIGHT anywhere under it --
                    // easy to hit once many chunks are merged into one large shape. Disabling
                    // it renders the outline above terrain unconditionally, which is what an
                    // informational territory overlay should do regardless of terrain height.
                    .depthTestEnabled(false);
            List<Shape> holeShapes = new ArrayList<>();
            for (List<Vector2d> hole : holes) {
                Shape holeShape = toShape(hole);
                if (holeShape != null) {
                    holeShapes.add(holeShape);
                }
            }
            if (!holeShapes.isEmpty()) {
                markerBuilder.holes(holeShapes.toArray(new Shape[0]));
            }
            markerSet.getMarkers().put(tribe.getId() + "_region_" + regionIndex, markerBuilder.build());

            // ShapeMarker/POIMarker labels only reveal on click in BlueMap's web UI, not
            // persistently -- an html-type marker is the documented way to get an always-visible
            // label, so place one at the region's center showing the tribe name.
            Vector2d centroid = centroidOf(region);
            HtmlMarker nameLabel = HtmlMarker.builder()
                    .label(tribe.getName())
                    .position(new Vector3d(centroid.getX(), MARKER_HEIGHT, centroid.getY()))
                    // BlueMap's anchor is a fixed pixel offset from the element's own top-left
                    // corner, which we can't know in advance since it depends on the rendered
                    // text length -- CSS transform: translate(-50%,-50%) centers the element on
                    // its own actual rendered size instead, correct for any tribe name length.
                    .html("<div style=\"color:#fff;background:rgba(0,0,0,0.55);padding:2px 6px;"
                            + "border-radius:3px;white-space:nowrap;font-weight:bold;pointer-events:none;"
                            + "transform:translate(-50%,-50%);\">"
                            + safeName + "</div>")
                    .anchor(0, 0)
                    .build();
            markerSet.getMarkers().put(tribe.getId() + "_label_" + regionIndex, nameLabel);
        }
    }

    // Groups a tribe's claimed chunks (already filtered to one dimension) into 4-directionally
    // connected components via BFS, so each contiguous block of territory gets one merged
    // outline instead of one rectangle per chunk.
    private static List<Set<ChunkPos>> groupIntoConnectedRegions(Set<ChunkPos> claims) {
        List<Set<ChunkPos>> regions = new ArrayList<>();
        Set<ChunkPos> remaining = new HashSet<>(claims);
        int[][] offsets = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        while (!remaining.isEmpty()) {
            ChunkPos start = remaining.iterator().next();
            Set<ChunkPos> region = new HashSet<>();
            List<ChunkPos> queue = new ArrayList<>();
            queue.add(start);
            remaining.remove(start);
            while (!queue.isEmpty()) {
                ChunkPos current = queue.remove(queue.size() - 1);
                region.add(current);
                for (int[] offset : offsets) {
                    ChunkPos neighbor = new ChunkPos(current.x + offset[0], current.z + offset[1]);
                    if (remaining.remove(neighbor)) {
                        queue.add(neighbor);
                    }
                }
            }
            regions.add(region);
        }
        return regions;
    }

    private record Edge(Vector2d from, Vector2d to) {
        Edge reversed() {
            return new Edge(to, from);
        }
    }

    // Traces the boundary of a connected set of unit chunk-cells into closed polygon loops:
    // each cell's 4 edges are walked clockwise, any edge shared with a same-region neighbor is
    // walked in the opposite direction by that neighbor and cancels out, leaving only the true
    // boundary edges, which are then chained start-to-end into loops. Produces one outer loop
    // plus a loop for each enclosed unclaimed pocket.
    //
    // Saddle vertices: where two claimed cells of the same region meet only corner-to-corner,
    // a single boundary vertex has TWO outgoing edges. Keying edges by their start vertex in a
    // plain Map would silently drop one of them and corrupt the chain into short, sometimes
    // 2-point loops -- which Shape.builder().build() rejects ("A shape has to have at least 3
    // points!"), previously taking the server down. This is not a contrived shape: skipping a
    // single chunk while walking a perimeter and later closing the ring produces it. So edges
    // are kept in a multimap and the saddle is resolved by continuing with the sharpest
    // clockwise turn, which keeps each loop's interior consistently on its right.
    private static List<List<Vector2d>> traceBoundaryLoops(Set<ChunkPos> region) {
        Set<Edge> allEdges = new HashSet<>();
        for (ChunkPos c : region) {
            double minX = c.getMinBlockX();
            double maxX = c.getMaxBlockX() + 1;
            double minZ = c.getMinBlockZ();
            double maxZ = c.getMaxBlockZ() + 1;
            Vector2d nw = new Vector2d(minX, minZ);
            Vector2d ne = new Vector2d(maxX, minZ);
            Vector2d se = new Vector2d(maxX, maxZ);
            Vector2d sw = new Vector2d(minX, maxZ);
            allEdges.add(new Edge(nw, ne));
            allEdges.add(new Edge(ne, se));
            allEdges.add(new Edge(se, sw));
            allEdges.add(new Edge(sw, nw));
        }

        // Multimap rather than Map<Vector2d, Vector2d>: a saddle vertex has two outgoing edges.
        Map<Vector2d, List<Vector2d>> boundary = new HashMap<>();
        for (Edge edge : allEdges) {
            if (!allEdges.contains(edge.reversed())) {
                boundary.computeIfAbsent(edge.from(), k -> new ArrayList<>()).add(edge.to());
            }
        }

        List<List<Vector2d>> loops = new ArrayList<>();
        while (!boundary.isEmpty()) {
            Vector2d start = boundary.keySet().iterator().next();
            List<Vector2d> loop = new ArrayList<>();
            Vector2d current = start;
            Vector2d previous = null;
            while (true) {
                List<Vector2d> outgoing = boundary.get(current);
                if (outgoing == null || outgoing.isEmpty()) {
                    break;
                }
                Vector2d next = pickSharpestClockwise(previous, current, outgoing);
                outgoing.remove(next);
                if (outgoing.isEmpty()) {
                    boundary.remove(current);
                }
                loop.add(current);
                previous = current;
                current = next;
                if (current.equals(start)) {
                    break;
                }
            }
            loops.add(loop);
        }
        return loops;
    }

    // Picks which edge to follow out of a vertex. Only ever ambiguous at a saddle (two claimed
    // cells meeting corner-to-corner). Cell edges are emitted clockwise with x running east and
    // z south, so a loop's interior lies to the right of travel; continuing with the sharpest
    // clockwise turn preserves that and keeps the two loops meeting at the saddle separate
    // rather than merging them.
    private static Vector2d pickSharpestClockwise(Vector2d previous, Vector2d current, List<Vector2d> outgoing) {
        if (outgoing.size() == 1 || previous == null) {
            return outgoing.get(0);
        }
        double inX = current.getX() - previous.getX();
        double inZ = current.getY() - previous.getY();
        Vector2d best = null;
        double bestTurn = Double.NEGATIVE_INFINITY;
        for (Vector2d candidate : outgoing) {
            double outX = candidate.getX() - current.getX();
            double outZ = candidate.getY() - current.getY();
            double cross = inX * outZ - inZ * outX; // positive == clockwise turn
            double dot = inX * outX + inZ * outZ;
            double turn = Math.atan2(cross, dot);   // (-pi, pi]; larger == more clockwise
            if (turn > bestTurn) {
                bestTurn = turn;
                best = candidate;
            }
        }
        return best;
    }

    // Returns null for a loop too short to be a polygon. The tracer shouldn't produce these any
    // more, but BlueMap throws rather than ignoring a degenerate shape, so this stays as a
    // cheap guarantee that a geometry edge case can never again escalate into a server crash.
    private static Shape toShape(List<Vector2d> loop) {
        if (loop.size() < 3) {
            return null;
        }
        Shape.Builder builder = Shape.builder();
        for (Vector2d point : loop) {
            builder.addPoint(point);
        }
        return builder.build();
    }

    private static double loopArea(List<Vector2d> loop) {
        double sum = 0;
        int n = loop.size();
        for (int i = 0; i < n; i++) {
            Vector2d a = loop.get(i);
            Vector2d b = loop.get((i + 1) % n);
            sum += a.getX() * b.getY() - b.getX() * a.getY();
        }
        return Math.abs(sum) / 2.0;
    }

    private static Vector2d centroidOf(Set<ChunkPos> region) {
        double sumX = 0;
        double sumZ = 0;
        for (ChunkPos c : region) {
            sumX += c.getMinBlockX() + 8;
            sumZ += c.getMinBlockZ() + 8;
        }
        return new Vector2d(sumX / region.size(), sumZ / region.size());
    }

    // Tribe names are already regex-restricted to [A-Za-z0-9_]{3,16} at creation time, so this
    // can't currently matter, but the html marker's content is inserted raw into the DOM with
    // no sanitization of its own -- escape defensively in case that validation ever loosens.
    private static String escapeHtml(String text) {
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    // Uses the tribe's custom color (/tribe set color) if set, otherwise a color
    // deterministically derived from the tribe's UUID so it's still stable across restarts.
    private static Color shade(Tribe tribe, float alpha) {
        if (tribe.hasColor()) {
            return new Color(tribe.getColor(), alpha);
        }
        float hue = (Math.abs(tribe.getId().hashCode()) % 360) / 360f;
        int rgb = java.awt.Color.HSBtoRGB(hue, 0.6f, 0.9f) & 0xFFFFFF;
        return new Color(rgb, alpha);
    }
}
