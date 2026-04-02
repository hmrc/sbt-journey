## TODOs

* Fix the `Reads` instances for generated journey models which contain `List`s.
* Rework the hmrc-frontend-scaffold.g8 template to use init_service package structure or create a new template
* Special care needs to be taken to preserve the changes to the frontend-scaffold:
  * Changed `RichJsObject` to use `deepMerge`, preserving nested answers
  * Changed `Page` to have `AnswerType`
    * Not sure whether this is the final change as scalac doesn't actually seem to check it
  * Changed `QuestionPage` to have `AnswerType` and `submitRoute`
    * May wish to add `loadRoute` too for convenience?
  * Added passthrough to `journey.Routes` in `prod.routes`
* Rework the generated code to make use of the package structure
* Implement switch-case else / default functionality?
* Consider whether to use the `journeys.<journey name>` to build a sub-package structure
  * e.g. uk.gov.hmrc.<service>.{controllers,forms,pages}.<journey>
  * Could even consider using a fractal application structure (e.g. uk.gov.hmrc.<service>.<journey>.{controllers,forms,pages})
  * This would lessen the chance of reverse routes collisions
  * However it isn't common practice in the rest of HMRC Digital
  * It could be done just for controllers rather than the entire application
  * If we don't do this, implement validation to warn users about potential class name collisions (e.g. same page name, different journey).
* Use `String#indent` rather than padding vars once we're on JDK12+ everywhere. At the moment we can't do this because sbt-settings sets `-java-output-version` to 11.
* More testing around nested models and form binding
* More testing around reuse of pages in different "forks" of a journey
* Add support for `onRemove` / `onDelete` endpoints for add-to-list journeys
* Migrate to using the `Imports` functionality rather than hardcoded imports as much as possible?