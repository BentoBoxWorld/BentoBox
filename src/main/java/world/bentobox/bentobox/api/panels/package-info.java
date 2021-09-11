/**
 * API for GUI panel creation and usage.
 *
 * <p>
 * BentoBox provides an API that enables Addon developers to display a GUI.
 * There is the basic Panel and the more advanced TabbedPanel. Both use Builders
 * to make them. For examples, look at the Settings classes.
 * </p>
 * <p>
 * Clicking on a panel item is handled by classes that implement the PanelListener interface.
 * When a click is made, the listener is called with various parameters available. It is possible
 * to have an overall listener for the whole panel and also individual listeners just for the
 * icon that was clicked.
 * <p>
 * The tabbed panel contains Tabs that are accessible via icons at the top of the panel.
 * </p>
 *
 * @author tastybento
 * @since 1.7.0
 */
package world.bentobox.bentobox.api.panels;