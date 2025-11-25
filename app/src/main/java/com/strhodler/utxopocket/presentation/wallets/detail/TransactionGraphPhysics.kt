package com.strhodler.utxopocket.presentation.wallets.detail

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import java.util.Random
import org.jbox2d.collision.shapes.CircleShape
import org.jbox2d.collision.shapes.EdgeShape
import org.jbox2d.common.Vec2
import org.jbox2d.dynamics.Body
import org.jbox2d.dynamics.BodyDef
import org.jbox2d.dynamics.BodyType
import org.jbox2d.dynamics.FixtureDef
import org.jbox2d.dynamics.World
import org.jbox2d.dynamics.joints.DistanceJointDef
import org.jbox2d.dynamics.joints.MouseJoint
import org.jbox2d.dynamics.joints.MouseJointDef

data class NodeLayout(
    val node: GraphNode,
    val center: Offset,
    val radiusPx: Float
) {
    fun contains(point: Offset): Boolean {
        val dx = point.x - center.x
        val dy = point.y - center.y
        return dx * dx + dy * dy <= (radiusPx + 12f) * (radiusPx + 12f)
    }
}

internal fun computeJBox2DLayout(
    graph: TransactionGraph,
    canvasSize: IntSize,
    composeDensity: Density
): List<NodeLayout> {
    if (canvasSize.width == 0 || canvasSize.height == 0) return emptyList()

    val pixelsPerMeter = (minOf(canvasSize.width, canvasSize.height) / 12f).coerceAtLeast(24f)
    val worldWidth = canvasSize.width / pixelsPerMeter
    val worldHeight = canvasSize.height / pixelsPerMeter
    val world = World(Vec2(0f, 0f))
    addBounds(world, worldWidth, worldHeight)

    val random = Random(graph.seed + canvasSize.width + canvasSize.height)
    val bodies = mutableMapOf<String, Body>()
    val radii = mutableMapOf<String, Float>()

    graph.nodes.forEach { node ->
        val radiusPx = radiusFor(node, composeDensity)
        radii[node.id] = radiusPx
        val bodyDef = BodyDef().apply {
            type = BodyType.DYNAMIC
            position = Vec2(
                0.5f + random.nextFloat() * (worldWidth - 1f),
                0.5f + random.nextFloat() * (worldHeight - 1f)
            )
            linearDamping = 6f
            angularDamping = 6f
        }
        val body = world.createBody(bodyDef)
        val shape = CircleShape().apply {
            m_radius = radiusPx / pixelsPerMeter
        }
        val fixture = FixtureDef().apply {
            this.shape = shape
            this.density = 0.6f
            this.friction = 0.2f
            this.restitution = 0.1f
        }
        body.createFixture(fixture)
        bodies[node.id] = body
    }

    graph.edges.forEach { edge ->
        val from = bodies[edge.from] ?: return@forEach
        val to = bodies[edge.to] ?: return@forEach
        val jointDef = DistanceJointDef().apply {
            initialize(from, to, from.position, to.position)
            frequencyHz = 2.2f
            dampingRatio = 0.85f
            val delta = Vec2(from.position)
            delta.subLocal(to.position)
            length = delta.length().coerceIn(1.5f, 6f)
        }
        world.createJoint(jointDef)
    }

    return graph.nodes.mapNotNull { node ->
        val body = bodies[node.id] ?: return@mapNotNull null
        val center = Offset(
            x = body.position.x * pixelsPerMeter,
            y = body.position.y * pixelsPerMeter
        )
        val radiusPx = radii[node.id] ?: return@mapNotNull null
        NodeLayout(node = node, center = center, radiusPx = radiusPx)
    }
}

private fun addBounds(world: World, width: Float, height: Float) {
    val ground = world.createBody(BodyDef())
    val bottom = EdgeShape().apply { set(Vec2(0f, 0f), Vec2(width, 0f)) }
    val top = EdgeShape().apply { set(Vec2(0f, height), Vec2(width, height)) }
    val left = EdgeShape().apply { set(Vec2(0f, 0f), Vec2(0f, height)) }
    val right = EdgeShape().apply { set(Vec2(width, 0f), Vec2(width, height)) }
    ground.createFixture(bottom, 0f)
    ground.createFixture(top, 0f)
    ground.createFixture(left, 0f)
    ground.createFixture(right, 0f)
}

internal data class PhysicsEngine(
    val world: World,
    val bodies: Map<String, Body>,
    val radii: Map<String, Float>,
    val pixelsPerMeter: Float,
    val ground: Body
)

internal fun buildPhysicsEngine(
    graph: TransactionGraph,
    canvasSize: IntSize,
    composeDensity: Density
): PhysicsEngine? {
    if (canvasSize.width == 0 || canvasSize.height == 0) return null
    val pixelsPerMeter = (minOf(canvasSize.width, canvasSize.height) / 12f).coerceAtLeast(24f)
    val worldWidth = canvasSize.width / pixelsPerMeter
    val worldHeight = canvasSize.height / pixelsPerMeter
    val world = World(Vec2(0f, 0f))
    val ground = world.createBody(BodyDef())
    addBounds(world, worldWidth, worldHeight)
    val random = Random(graph.seed + canvasSize.width + canvasSize.height)
    val bodies = mutableMapOf<String, Body>()
    val radii = mutableMapOf<String, Float>()
    graph.nodes.forEach { node ->
        val radiusPx = radiusFor(node, composeDensity)
        radii[node.id] = radiusPx
        val bodyDef = BodyDef().apply {
            type = BodyType.DYNAMIC
            position = Vec2(
                0.5f + random.nextFloat() * (worldWidth - 1f),
                0.5f + random.nextFloat() * (worldHeight - 1f)
            )
            linearDamping = 6f
            angularDamping = 6f
        }
        val body = world.createBody(bodyDef)
        val shape = CircleShape().apply {
            m_radius = radiusPx / pixelsPerMeter
        }
        val fixture = FixtureDef().apply {
            this.shape = shape
            this.density = 0.6f
            this.friction = 0.2f
            this.restitution = 0.1f
        }
        body.createFixture(fixture)
        bodies[node.id] = body
    }
    graph.edges.forEach { edge ->
        val from = bodies[edge.from] ?: return@forEach
        val to = bodies[edge.to] ?: return@forEach
        val jointDef = DistanceJointDef().apply {
            initialize(from, to, from.position, to.position)
            frequencyHz = 2.2f
            dampingRatio = 0.85f
            val delta = Vec2(from.position)
            delta.subLocal(to.position)
            length = delta.length().coerceIn(1.5f, 6f)
        }
        world.createJoint(jointDef)
    }
    graph.nodes.forEach { node -> bodies[node.id]?.userData = node }
    return PhysicsEngine(
        world = world,
        bodies = bodies,
        radii = radii,
        pixelsPerMeter = pixelsPerMeter,
        ground = ground
    )
}

internal fun PhysicsEngine.layouts(): List<NodeLayout> =
    bodies.mapNotNull { (id, body) ->
        val radius = radii[id] ?: return@mapNotNull null
        NodeLayout(
            node = body.userData as? GraphNode ?: return@mapNotNull null,
            center = Offset(body.position.x * pixelsPerMeter, body.position.y * pixelsPerMeter),
            radiusPx = radius
        )
    }

internal fun PhysicsEngine.attachNodeData(nodes: List<GraphNode>) {
    nodes.forEach { node ->
        bodies[node.id]?.userData = node
    }
}

internal fun PhysicsEngine.createMouseJoint(nodeId: String, targetOffset: Offset): MouseJoint? {
    val body = bodies[nodeId] ?: return null
    val def = MouseJointDef().apply {
        this.bodyA = ground
        this.bodyB = body
        this.target.set(targetOffset.x / pixelsPerMeter, targetOffset.y / pixelsPerMeter)
        maxForce = 12000f * body.mass
        dampingRatio = 0.85f
        frequencyHz = 6f
        body.isAwake = true
    }
    return world.createJoint(def) as? MouseJoint
}
